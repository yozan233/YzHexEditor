package com.peke.hex.editor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class Md5Utils {

    public static String getMD5(File file){
        return getMD5(file,0);
    }

    public static String getMD5(File file, int startBitIndex){
        FileInputStream stream;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return  null;
        }

        return getMD5(stream,startBitIndex);
    }

    public static String getMD5(InputStream stream){
        return getMD5(stream,0);
    }

    public static String getMD5(InputStream stream, int startBitIndex){
        int len = 0;
        MessageDigest digest = null;
        byte[] buffer = new byte[1024];
        try {
            digest = MessageDigest.getInstance("MD5");
            if (startBitIndex > 0)
                stream.skip(startBitIndex);
            while ((len = stream.read(buffer)) != -1){
                digest.update(buffer,0,len);
            }
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte[] res = digest.digest();
        BigInteger bigInteger = new BigInteger(1, res);
        return bigInteger.toString(16);
    }

    public static String getMD5(byte[] bytes){
        byte[] res = getMD5Bytes(bytes);
        BigInteger bigInteger = new BigInteger(1, res);
        return bigInteger.toString(16);
    }

    public static byte[] getMD5Bytes(byte[] bytes){
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(bytes,0,bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[]{0};
        }
        return digest.digest();
    }


}
