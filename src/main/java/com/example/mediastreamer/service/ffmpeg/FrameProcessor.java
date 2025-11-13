package com.example.mediastreamer.service.ffmpeg;

import com.example.mediastreamer.model.VideoMetadata;
import com.example.mediastreamer.service.minio.MinIoService;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.example.mediastreamer.utils.Constants.CHUNK;
import static com.example.mediastreamer.utils.Constants.CHUNK_SECONDS;
import static com.example.mediastreamer.utils.Constants.DOT;
import static com.example.mediastreamer.utils.Constants.FRAGMENTED_KEY_FRAMES;
import static com.example.mediastreamer.utils.Constants.MICROSECOND_TO_SECOND_MULTIPLIER;
import static com.example.mediastreamer.utils.Constants.MOV_FLAGS;
import static com.example.mediastreamer.utils.Constants.RESOLUTION;

@Service
public class FrameProcessor {
    @Autowired
    private MinIoService minIoService;

    private static RateLimiter rateLimiter = RateLimiter.create(300);

    private final Map<Integer, Integer> resolutionMap = ImmutableMap.of(
      2160, 1440,
      1440, 1080,
      1080, 720,
      720,480
    );

    public void chunkVideos(String videoId) {
        VideoMetadata videoMetadata = minIoService.downloadVideoMetadata(videoId);
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(minIoService.getInputStreamForVideo(videoId))) {
            TreeSet<String> chunks = new TreeSet<>();
            grabber.start();
            videoMetadata.setDuration(grabber.getLengthInTime() / MICROSECOND_TO_SECOND_MULTIPLIER);
            long totalTime = grabber.getLengthInTime();
            long curTime = 0;
            int index = 0;
            long chunkMicroSec = (long) (CHUNK_SECONDS * MICROSECOND_TO_SECOND_MULTIPLIER);
            while (curTime < totalTime) {
                long chunkSize = Math.min(chunkMicroSec, totalTime - curTime);
                copyFramesWithMultipleResolutions(videoMetadata, curTime, index,
                        chunkSize, chunks, grabber.getImageWidth(), grabber.getImageHeight());
                curTime += chunkSize;
                index++;
            }
            grabber.stop();
            grabber.release();
            videoMetadata.setChunks(chunks.stream().toList());
            minIoService.uploadVideoMetadata(videoMetadata);
        } catch (Exception e) {
            System.out.println("Exception occurred while processing Frames!");
            e.printStackTrace();
        }
    }

    public void copyFramesWithMultipleResolutions(VideoMetadata videoMetadata, long finalCurTime, int finalIndex,
                                                  long chunkSize, Set<String> chunks, int imageWidth, int imageHeight) {
        copyFrames(videoMetadata, finalCurTime,
                chunkSize, finalIndex, chunks, new int[]{imageWidth, imageHeight});
        int[] reducedResolution = reduceResolution(imageWidth, imageHeight);
        if (reducedResolution[1] != -1) {
            copyFrames(videoMetadata, finalCurTime,
                    chunkSize, finalIndex, chunks, reducedResolution);
        }
    }

    public synchronized void copyFrames(VideoMetadata videoMetadata,
                          long offset, long length, int index, Set<String> chunks, int[] resolution) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(minIoService.getInputStreamForVideo(videoMetadata.getVideoId()))) {
            grabber.start();
            grabber.setVideoTimestamp(offset);
            PipedOutputStream pout = new PipedOutputStream();
            PipedInputStream pin = new PipedInputStream(pout);
            FFmpegFrameRecorder recorder = getNewRecorder(videoMetadata, grabber, index, chunks, pin, pout, resolution);
            recorder.start();
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Frame frame;
            while ((frame = grabber.grabFrame()) != null && grabber.getTimestamp() < offset + length) {
                rateLimiter.acquire();
                // Resize each frame for lower resolution.
                if (resolution[1] != grabber.getImageHeight()) {
                    Mat mat = converter.convert(frame);
                    if (mat != null && !mat.empty()) {
                        Mat resized = new Mat();
                        opencv_imgproc.resize(mat, resized, new Size(resolution[0], resolution[1]));
                        Frame resizedFrame = converter.convert(resized);
                        recorder.record(resizedFrame);
                        resized.release();
                    }
                } else {
                    recorder.record(frame);
                }
            }
            grabber.stop();
            grabber.release();
            recorder.stop();
            recorder.release();
            pout.close();
            pin.close();
        } catch (Exception e) {
            System.out.println("Exception occurred while copying Frames! chunk number: " + index + " resolution: " + resolution[1]);
            e.printStackTrace();
        }
    }

    public FFmpegFrameRecorder getNewRecorder(VideoMetadata videoMetadata, FFmpegFrameGrabber grabber,
                                              int index, Set<String> chunks, PipedInputStream pin,
                                              PipedOutputStream pout, int[] resolution) throws IOException {
        String chunkId = new StringBuffer()
                .append(videoMetadata.getVideoId())
                .append(RESOLUTION)
                .append(resolution[1])
                .append(CHUNK)
                .append(index)
                .append(DOT)
                .append(videoMetadata.getExtension())
                .toString();
        chunks.add(chunkId);

        // Using independent thread because if all threads in executor service is full
        // it can lead to a race condition where upload is waiting for data from recorder
        // but recorder is not yet scheduled because all threads in executor service are
        // busy.
        Thread minIoUploader = new Thread(() -> minIoService.uploadVideoChunkUsingThreadStream(pin, chunkId));
        minIoUploader.start();

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(pout, resolution[0],
                resolution[1], grabber.getAudioChannels());
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

    // This is just to simulate lower quality. There are better ways to downscale
    // video for better network speed.
    public int[] reduceResolution(int imageWidth, int imageHeight) {
        double aspectRatio = (double) imageWidth / imageHeight;
        int height = resolutionMap.getOrDefault(imageHeight, -1);
        return new int[]{(int) (height * aspectRatio), height};
    }
}