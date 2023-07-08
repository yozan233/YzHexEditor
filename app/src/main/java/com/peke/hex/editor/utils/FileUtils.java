package com.peke.hex.editor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static void copyFile(File srcFile, File dstFile) {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(srcFile);
            outputStream = new FileOutputStream(dstFile);
            byte[] buffer = new byte[1024*1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream,outputStream);
        }
    }


}
