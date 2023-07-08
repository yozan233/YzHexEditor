package com.peke.hex.editor.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class DisplayUtils {

    /**
     * 获取屏幕高度(px)
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    /**
     * 获取屏幕宽度(px)
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private static final int BASE_SW_DP = 360;

    public static int dp2px(Context context, float dp){
        Resources resources = context.getResources();
        int swDp = resources.getConfiguration().smallestScreenWidthDp;
        float value = dp * swDp / BASE_SW_DP;
        float f = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,value,resources.getDisplayMetrics());
        return (int) ((f >= 0) ? (f + 0.5f) : (f - 0.5f));
    }

    public static int sysDp2px(Context context,float dp){
        float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

}
