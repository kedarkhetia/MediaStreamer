package com.example.mediastreamer.service.minio;

import org.jcodec.common.io.SeekableByteChannel;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MinIOSeekableByteChannel implements SeekableByteChannel {

    @Autowired
    private MinIoService minIoService;

    private final String videoId;
    private final long totalSize;
    private long position = 0;
    private InputStream currentStream;
    private boolean isOpen;

    public MinIOSeekableByteChannel(String videoId, long totalSize) {
        this.videoId = videoId;
        this.totalSize = totalSize;
        this.isOpen = true;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel setPosition(long newPosition) throws IOException {
        this.position = newPosition;
        if (currentStream != null)
            currentStream.close();
        currentStream = null; // Will reopen at new offset
        return this;
    }

    @Override
    public long size() {
        return totalSize;
    }

    @Override
    public SeekableByteChannel truncate(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (currentStream == null) {
            currentStream = minIoService.getInputStreamForVideo(videoId, position);
        }
        byte[] data = new byte[dst.remaining()];
        int bytesRead = currentStream.read(data);
        if (bytesRead == -1)
            return -1;
        dst.put(data, 0, bytesRead);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public int write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() {
        isOpen = false;
    }
}
