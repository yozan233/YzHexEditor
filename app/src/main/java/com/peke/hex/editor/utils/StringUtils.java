package com.peke.hex.editor.utils;

public class StringUtils {

    /**
     * 字符串前加0
     * @param str 原字符串
     * @param len 加0后字符串长度
     * @return 加0后的字符串
     */
    public static String addZeroBeforeL(String str, int len){
        int oldLen = str.length();
        if (oldLen >= len)
            return str;
        else
            return addZeroBefore(str,len-oldLen);
    }

    /**
     * 字符串前加0
     * @param str 原字符串
     * @param n 加n个0
     * @return 加0后的字符串
     */
    public static String addZeroBefore(String str, int n){
        StringBuilder strBuilder = new StringBuilder(str);
        for (int i = 0; i < n; i++){
            strBuilder.insert(0, "0");
        }
        str = strBuilder.toString();
        return str;
    }

    /**
     * hex数据字符串数据转字节数组
     */
    public static byte[] hexDataToBytes(String dataStr){
        if (dataStr.length() % 2 != 0)
            dataStr += "0";
        byte[] bytes = new byte[dataStr.length()/2];
        for (int i=0,j=0;j<dataStr.length();i++,j+=2){
            int tint = Integer.parseInt(dataStr.substring(j,j+2),16);
            bytes[i] = (byte)tint;
        }
        return bytes;
    }


}
