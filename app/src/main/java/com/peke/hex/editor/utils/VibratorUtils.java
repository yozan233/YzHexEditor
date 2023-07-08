package com.peke.hex.editor.utils;

import android.content.Context;
import android.os.Vibrator;

/**
 * 震动工具类
 */
public class VibratorUtils {

    public static void vibrate(Context context,long milliseconds){
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(milliseconds);
        }
    }

    public static Vibrator getVibrator(Context context){
        return  (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

}
