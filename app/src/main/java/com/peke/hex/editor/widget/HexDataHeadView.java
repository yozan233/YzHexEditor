package com.peke.hex.editor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;

public class HexDataHeadView extends BaseHexDataView {

    private static final int textColor = Color.parseColor("#656565");

    private String encodingName = "ASCII";

    public HexDataHeadView(Context context) {
        this(context, null);
    }

    public HexDataHeadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HexDataHeadView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HexDataHeadView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        initialize(attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        textPaint.setColor(textColor);



    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Offset",getWordWith(2f),getWordCenterPosY(),textPaint);

        drawNumber(canvas);

        canvas.drawText(encodingName,getWordWith(33.5f),getWordCenterPosY(),textPaint);

    }

    private void drawNumber(Canvas canvas){
        StringBuilder numberBuilder = new StringBuilder();
        for (int i=0;i<8;i++){
            numberBuilder.append(i).append("  ");
        }
        canvas.drawText(numberBuilder.toString(), getWordWith(10f), getWordCenterPosY(), textPaint);
    }

}
