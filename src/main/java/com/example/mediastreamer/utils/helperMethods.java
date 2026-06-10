package com.example.mediastreamer.utils;

import java.io.File;

public class helperMethods {

    public static void cleanUpTempDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                file.delete();
            }
        }
        directory.delete();
    }
}
