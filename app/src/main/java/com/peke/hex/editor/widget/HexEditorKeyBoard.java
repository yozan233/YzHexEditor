package com.peke.hex.editor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.peke.hex.editor.R;
import com.peke.hex.editor.utils.DisplayUtils;

public class HexEditorKeyBoard extends CustomKeyBoardView {

    private static final InputKeyConfig[][] InputKeyConfigs =  {
            { //第一行
                new InputKeyConfig("A",InputKey.KEY_A),
                new InputKeyConfig("开头",InputKey.KEY_START,false),
                new InputKeyConfig("末尾",InputKey.KEY_END,false),
                new InputKeyConfig("←",InputKey.KEY_LEFT),
                new InputKeyConfig("→",InputKey.KEY_RIGHT)
            },
            { //第二行
                new InputKeyConfig("B",InputKey.KEY_B),
                new InputKeyConfig("1",InputKey.KEY_1),
                new InputKeyConfig("2",InputKey.KEY_2),
                new InputKeyConfig("3",InputKey.KEY_3),
                new InputKeyConfig("↑",InputKey.KEY_UP)
            },
            { //第三行
                new InputKeyConfig("C",InputKey.KEY_C),
                new InputKeyConfig("4",InputKey.KEY_4),
                new InputKeyConfig("5",InputKey.KEY_5),
                new InputKeyConfig("6",InputKey.KEY_6),
                new InputKeyConfig("↓",InputKey.KEY_DOWN)
            },
            { //第四行
                new InputKeyConfig("D",InputKey.KEY_D),
                new InputKeyConfig("7",InputKey.KEY_7),
                new InputKeyConfig("8",InputKey.KEY_8),
                new InputKeyConfig("9",InputKey.KEY_9),
                new InputKeyConfig("s",InputKey.KEY_ENTRY,false)
            },
            { //第五行
                new InputKeyConfig("E",InputKey.KEY_E),
                new InputKeyConfig("F",InputKey.KEY_F),
                new InputKeyConfig("0",InputKey.KEY_0),
                new InputKeyConfig("S",InputKey.KEY_ENTRY,false),
                new InputKeyConfig("",InputKey.KEY_ENTRY,false)
            }
    };

    public HexEditorKeyBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected InputKeyConfig[][] getInputKeyConfigs(){
        return InputKeyConfigs;
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mContext.getResources().getColor(R.color.grey_eeee));

        int mItemMargin = getItemMargin();
        float mItemWidth = getItemWidth();
        float mItemHeight = getItemHeight();
        Paint mTextPaint = getTextPaint();

        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        float textCenterPosY = (mItemHeight - fontMetrics.bottom + fontMetrics.top) / 2f - fontMetrics.top;

        for (int i=0;i<InputKeyConfigs.length;i++){
            InputKeyConfig[] inputKeyConfig = InputKeyConfigs[i];
            for (int j=0;j<inputKeyConfig.length;j++){
                InputKeyConfig keyConfig = inputKeyConfig[j];
                String keyName = keyConfig.name;
                if (keyName.length() == 0)
                    continue;
                float itemLeft = mItemWidth * j + mItemMargin * (j + 1);
                float itemTop = mItemHeight * i + mItemMargin * (i + 1);
                RectF rect;
                switch (keyName){
                    case "s":
                        rect = new RectF(itemLeft, itemTop, itemLeft + mItemWidth, itemTop + mItemHeight +mItemMargin);
                        drawItemBackground(canvas,getKeyBtnBg(keyName),rect);
                        break;
                    case "S":
                        rect = new RectF(itemLeft, itemTop, itemLeft + mItemWidth *2+mItemMargin, itemTop + mItemHeight);
                        drawItemBackground(canvas,getKeyBtnBg(keyName),rect);
                        canvas.drawText("确定", mItemWidth *1.5f+ itemLeft, mItemHeight /2 + itemTop,mTextPaint);
                        break;
                    default:
                        rect = new RectF(itemLeft, itemTop, itemLeft + mItemWidth, itemTop + mItemHeight);
                        drawItemBackground(canvas,getKeyBtnBg(keyName),rect);
                        canvas.drawText(keyName,rect.centerX(), textCenterPosY + itemTop,mTextPaint);
                        break;
                }
                keyConfig.rect = rect;
            }
        }
    }

    private Drawable getKeyBtnBg(String name){
        boolean checked = isKeyTouchDown(name);
        float dp5 = DisplayUtils.dp2px(mContext,5);
        int color = checked ? R.color.grey_dddd : R.color.white_f8f8;
        switch (name){
            case "s":
                return createButtonBackground(color)
                        .topLeftRadius(dp5)
                        .topRightRadius(dp5)
                        .build();
            case "S":
                return createButtonBackground(color)
                        .topLeftRadius(dp5)
                        .bottomLeftRadius(dp5)
                        .bottomRightRadius(dp5)
                        .build();
            default:
                return createButtonBackground(color)
                        .radius(dp5)
                        .build();
        }
    }



}
