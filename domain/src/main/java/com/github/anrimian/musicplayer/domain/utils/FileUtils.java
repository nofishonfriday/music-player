package com.github.anrimian.musicplayer.domain.utils;

import java.io.File;

import static com.github.anrimian.musicplayer.domain.utils.TextUtils.isEmpty;

public class FileUtils {

    public static String getParentDirPath(String path) {
        int lastSeparatorIndex = path.lastIndexOf("/");
        if (lastSeparatorIndex != -1) {
            return path.substring(0, lastSeparatorIndex);
        }
        return path;
    }

    public static String getFileName(String path) {
        String displayPath = path;
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            displayPath = path.substring(++lastSlashIndex);
        }
        return displayPath;
    }

    public static String formatFileName(String filePath) {
        return formatFileName(filePath, false);
    }

    public static String formatFileName(String filePath, boolean showExtension) {
        if (isEmpty(filePath)) {
            return "";
        }
        String fileName = getFileName(filePath);
        if (!showExtension) {
            int cropIndex = fileName.lastIndexOf('.');
            if (cropIndex != -1) {
                return fileName.substring(0, cropIndex);
            }
        }
        return fileName;
    }

    public static String getChangedFilePath(String fullPath, String newFileName) {
        String fileName = FileUtils.formatFileName(fullPath);
        String newPath = fullPath.replace(fileName, newFileName);
        return getUniqueFilePath(newPath, newFileName);
    }

    private static String getUniqueFilePath(String filePath, String fileName) {
        File file = new File(filePath);
        int filesCount = 0;
        String newFileName;
        if (file.exists() && !file.isDirectory()) {
            while (file.exists()) {
                filesCount++;

                newFileName = fileName + "(" + filesCount + ")";//hmm, check on new name like name(1)?
                String newPath = filePath.replace(fileName, newFileName);

                file = new File(newPath);
            }
        }
        return file.getAbsolutePath();
    }
}
