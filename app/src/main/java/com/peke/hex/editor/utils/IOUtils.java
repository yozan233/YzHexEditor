package com.peke.hex.editor.utils;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable c:closeables){
            if (c == null)
                continue;
            try {
                c.close();
            } catch (IOException ignored) { }
        }
    }



}
