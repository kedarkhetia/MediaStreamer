package com.example.mediastreamer.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MediaUtils {

    public static String getVideoResolution(String videoUrlOrPath) {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v"); command.add("error");
        command.add("-select_streams"); command.add("v:0");
        command.add("-show_entries"); command.add("stream=width,height");
        command.add("-of"); command.add("csv=s=x:p=0");
        command.add(videoUrlOrPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String resolution = reader.readLine();
                if (resolution != null) {
                    return resolution.trim();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("ffprobe failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
