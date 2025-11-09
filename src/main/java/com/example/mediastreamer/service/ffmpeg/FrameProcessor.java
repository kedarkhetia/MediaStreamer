package com.example.mediastreamer.service.ffmpeg;

import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.example.mediastreamer.utils.Constants.CHUNK;
import static com.example.mediastreamer.utils.Constants.CHUNK_SECONDS;
import static com.example.mediastreamer.utils.Constants.DOT;
import static com.example.mediastreamer.utils.Constants.FRAGMENTED_KEY_FRAMES;
import static com.example.mediastreamer.utils.Constants.MICROSECOND_TO_SECOND_MULTIPLIER;
import static com.example.mediastreamer.utils.Constants.MOV_FLAGS;
import static com.example.mediastreamer.utils.Constants.THREAD_TIMEOUT;

@Service
public class FrameProcessor {
    @Autowired
    private MinIoService minIoService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public void chunkVideos(String videoId) {
        VideoMetadata videoMetadata = minIoService.downloadVideoMetadata(videoId);
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(minIoService.getInputStreamForVideo(videoId))) {
            TreeSet<String> chunks = new TreeSet<>();
            List<Future<?>> futures = new LinkedList<>();
            grabber.start();
            videoMetadata.setDuration(grabber.getLengthInTime() / MICROSECOND_TO_SECOND_MULTIPLIER);
            long totalTime = grabber.getLengthInTime();
            long curTime = 0;
            int index = 0;
            long chunkMicroSec = (long) (CHUNK_SECONDS * MICROSECOND_TO_SECOND_MULTIPLIER);
            while (curTime < totalTime) {
                long chunkSize = Math.min(chunkMicroSec, totalTime - curTime);
                long finalCurTime = curTime;
                int finalIndex = index;
                Thread copyFramesParallel = new Thread(() -> copyFrames(videoId, videoMetadata, finalCurTime,
                        chunkSize, finalIndex, chunks));
                futures.add(executorService.submit(copyFramesParallel));
                curTime += chunkSize;
                index++;
            }
            grabber.stop();
            grabber.release();
            synchronized (this) {
                while (!futures.stream().allMatch(Future::isDone)) {
                    this.wait(THREAD_TIMEOUT);
                }
            }
            executorService.shutdown();
            videoMetadata.setChunks(chunks.stream().toList());
            minIoService.uploadVideoMetadata(videoMetadata);
        } catch (Exception e) {
            System.out.println("Exception occurred while processing Frames!");
            e.printStackTrace();
        }
    }

    public void copyFrames(String videoId, VideoMetadata videoMetadata,
                          long offset, long length, int index, Set<String> chunks) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(minIoService.getInputStreamForVideo(videoId))) {
            grabber.start();
            grabber.setVideoTimestamp(offset);
            FFmpegFrameRecorder recorder = getNewRecorder(videoMetadata, grabber, index, chunks);
            recorder.start();
            Frame frame;
            while ((frame = grabber.grabFrame()) != null && grabber.getTimestamp() < offset + length) {
                recorder.record(frame);
            }
            grabber.stop();
            grabber.release();
            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            System.out.println("Exception occurred while copying Frames!");
            e.printStackTrace();
        }
    }

    public FFmpegFrameRecorder getNewRecorder(VideoMetadata videoMetadata, FFmpegFrameGrabber grabber,
                                              int index, Set<String> chunks) throws IOException {
        String chunkId = new StringBuffer()
                .append(videoMetadata.getVideoId())
                .append(CHUNK)
                .append(index)
                .append(DOT)
                .append(videoMetadata.getExtension())
                .toString();
        chunks.add(chunkId);
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        Thread minIoUploader = new Thread(() -> minIoService.uploadVideoChunkUsingThreadStream(pin, chunkId));
        executorService.submit(minIoUploader);
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(pout, grabber.getImageWidth(),
                grabber.getImageHeight(), grabber.getAudioChannels());
        configureFrameRecorder(videoMetadata.getExtension(), recorder, grabber);
        return recorder;
    }

    public void configureFrameRecorder(String extension, FFmpegFrameRecorder recorder, FFmpegFrameGrabber grabber) {
        recorder.setFormat(extension);
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setAudioBitrate(grabber.getAudioBitrate());
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setAudioCodec(grabber.getAudioCodec());
        //recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        //recorder.setPixelFormat(grabber.getPixelFormat());
        recorder.setSampleRate(grabber.getSampleRate());
        recorder.setOption(MOV_FLAGS, FRAGMENTED_KEY_FRAMES);
    }
}