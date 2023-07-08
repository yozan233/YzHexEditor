package com.peke.hex.editor.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 正方形的LinearLayout
 */
public class SquareLinearLayout extends LinearLayout {
    public SquareLinearLayout(Context context) {
        this(context,null);
    }
    public SquareLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }
    public SquareLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 先让父类计算宽度和高度
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 以父类计算的高度获取我们想要的宽度
        int width = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
        int height = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
        int maxSize = Math.max(width,height);
        // 让父类重新计算
        super.onMeasure(maxSize, maxSize);
    }

}