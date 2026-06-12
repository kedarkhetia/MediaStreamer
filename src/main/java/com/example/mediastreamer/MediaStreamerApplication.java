package com.example.mediastreamer;

import com.example.mediastreamer.mediaProcessor.MediaChunker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MediaStreamerApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(MediaStreamerApplication.class, args);
		// Initialize media chunker bean.
		MediaChunker mediaChunker = context.getBean(MediaChunker.class);
		mediaChunker.start();
	}
}
