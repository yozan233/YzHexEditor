package com.peke.hex.editor.widget;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.peke.hex.editor.utils.DisplayUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseHexDataView extends View {

    protected Context context;

    private static final Map<Float,Float> sWordWithMap = new HashMap<>();
    private static float sWordWidth = 0;
    private static float sTextSize = 0;
    private float sWordCenterPosY = 0;

    protected Paint textPaint;
    protected Paint backgroundPaint;
    protected Paint linePaint;

    private RectF offsetRect;
    private RectF dataTextRect;
    private RectF dataAndCharsRect;


    public BaseHexDataView(Context context) {
        this(context, null);
    }

    public BaseHexDataView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseHexDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BaseHexDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        initialize(attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        //等宽字体
        Typeface sansMonoTtf = Typeface.createFromAsset(context.getAssets(), "droid_sans_mono.ttf");

        textPaint = new Paint();
        textPaint.setTypeface(sansMonoTtf);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(DisplayUtils.dp2px(context,16));
        adjustTextSize();

        backgroundPaint = new Paint();

        linePaint = new Paint();


    }

    private void adjustTextSize() {
        if (sTextSize == 0){
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

            float trySize = textPaint.getTextSize();

            String text = "00000000 AA AA AA AA AA AA AA AA  AAAAAAAA";

            while (textPaint.measureText(text) > screenWidth) {
                trySize--;
                textPaint.setTextSize(trySize);
            }
            sTextSize = trySize;
        }
        textPaint.setTextSize(sTextSize);
    }

    /**
     * 文本下标转为半字节数据下标
     */
    public static int textIndex2DataIndex(int i){
        switch (i){
            case 0:
                return 0;
            case 1:
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
            case 5:
                return 3;
            case 6:
                return 4;
            case 7:
            case 8:
                return 5;
            case 9:
                return 6;
            case 10:
            case 11:
                return 7;
            case 12:
                return 8;
            case 13:
            case 14:
                return 9;
            case 15:
                return 10;
            case 16:
            case 17:
                return 11;
            case 18:
                return 12;
            case 19:
            case 20:
                return 13;
            case 21:
                return 14;
            case 22:
                return 15;
            default:
                return 16;
        }
    }

    /**
     * 半字节数据下标转为文本下标
     */
    public static int dataIndex2TextIndex(int i){
        switch (i){
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 6;
            case 5:
                return 7;
            case 6:
                return 9;
            case 7:
                return 10;
            case 8:
                return 12;
            case 9:
                return 13;
            case 10:
                return 15;
            case 11:
                return 16;
            case 12:
                return 18;
            case 13:
                return 19;
            case 14:
                return 21;
            case 15:
                return 22;
            default:
                return 23;
        }
    }

    public static boolean isSpaceIndex(int i){
        return i >= 3 && i % 3 == 0;
    }

    public float getWordWith(){
        return getWordWith(1);
    }

    public float getWordWith(float len){
        if (sWordWidth == 0){
            sWordWidth = textPaint.measureText(" ");
        }
        Float width = sWordWithMap.get(len);
        if (width == null){
            width = sWordWidth * len;
            sWordWithMap.put(len,width);
        }
        return width;
    }

    protected float getWordCenterPosY(){
        if (sWordCenterPosY == 0){
            int viewHeight = getMeasuredHeight();
            Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
            sWordCenterPosY = (viewHeight - fontMetrics.bottom + fontMetrics.top) / 2f - fontMetrics.top;
        }
        return sWordCenterPosY;
    }

    protected RectF getOffsetRect() {
        if (offsetRect == null){
            offsetRect = new RectF(0,0,getWordWith(9),getHeight());
        }
        return offsetRect;
    }

    protected RectF getDataAndCharsRect() {
        if (dataAndCharsRect == null){
            dataAndCharsRect = new RectF(getWordWith(9),0,getWidth(),getHeight());
        }
        return dataAndCharsRect;
    }

    public RectF getDataTextRect(){
        if (dataTextRect == null){
            dataTextRect = new RectF(
                    getWordWith(9.5f),
                    0,
                    getWordWith(32.5f),
                    getHeight()
            );
        }
        return dataTextRect;
    }

    protected float dp2px(float dp){
        float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp*scale+0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                DisplayUtils.dp2px(context, 28));
    }

}
