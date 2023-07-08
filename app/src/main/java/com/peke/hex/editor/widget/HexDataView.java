package com.peke.hex.editor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Range;
import android.view.MotionEvent;

import com.peke.hex.editor.bean.ByteBean;
import com.peke.hex.editor.utils.StringUtils;
import com.peke.hex.editor.utils.VibratorUtils;

public class HexDataView extends BaseHexDataView {

    private int offset = 0;
    private ByteBean[] data = new ByteBean[8];
    private int dataMaxRange = 0;

    //半字节的下标 0-16
    private Range<Integer> mDataSelectedRange = new Range<>(-1,-1);

    private OnClickListener onDataClickListener;
    private OnClickListener onDataLongClickListener;

    private static final int offsetTextColor = Color.parseColor("#7F7F7F");
    private static final int dataTextColor = Color.parseColor("#333333");
    private static final int dataModifiedTextColor = Color.parseColor("#FCA81F");

    private static final int offsetBackgroundColor1 = Color.parseColor("#EEEEEE");
    private static final int offsetBackgroundColor2 = Color.parseColor("#E5E5E5");

    private static final int dataBackgroundColor1 = Color.parseColor("#FFFFFF");
    private static final int dataBackgroundColor2 = Color.parseColor("#E5F0F8");

    private static final int selectedBackgroundColor = Color.parseColor("#4D00A5FD");
    private static final int selectedBackgroundColor2 = Color.parseColor("#4D54C2FD");

    private static final int lineColor = Color.parseColor("#DDDDDD");

    public HexDataView(Context context) {
        this(context, null);
    }

    public HexDataView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HexDataView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HexDataView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        initialize(attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        linePaint.setColor(lineColor);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setData(ByteBean[] data) {
        this.data = data;
        int i = 0;
        for (; i < 8; ++i) {
            ByteBean bean = i < data.length ? data[i] : null;
            if (bean == null)
                break;
        }
        dataMaxRange = i * 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);

        drawSelectedRange(canvas);

        drawLine(canvas);

        //8+1
        drawOffset(canvas);

        drawData(canvas);


    }

    private void drawOffset(Canvas canvas){
        String offsetStr = StringUtils.addZeroBeforeL(Integer.toHexString(offset).toUpperCase(),8);
        textPaint.setColor(offsetTextColor);
        canvas.drawText(offsetStr, getWordWith(0.5f), getWordCenterPosY(), textPaint);
    }

    private void drawData(Canvas canvas){
        float dataStrX = 9.5f;
        float charsStrX = 33.5f;
        for (int i=0;i<8;i++){
            ByteBean bean = i < data.length ? data[i] : null;
            if (bean != null){
                int hex = bean.value & 0xFF;
                boolean modified = bean.modified;
                String hexStr = StringUtils.addZeroBeforeL(Integer.toHexString(hex).toUpperCase(),2);
                if (i > 0)
                    dataStrX += 1;
                drawString(canvas,getWordWith(dataStrX),hexStr,modified?dataModifiedTextColor:dataTextColor);
                dataStrX += 2;

                String ch;
                if (hex <= 0x20 || hex == 0x7F)
                    ch = " ";
                else
                    ch = String.valueOf((char)hex);

                drawString(canvas,getWordWith(charsStrX),ch,modified?dataModifiedTextColor:dataTextColor);
                charsStrX ++;

            }
            else {
                if (i > 0)
                    dataStrX += 1;
                drawString(canvas,getWordWith(dataStrX),"  ",dataTextColor);
                dataStrX ++;
                charsStrX ++;
            }
        }

    }

    private void drawString(Canvas canvas,float textStart,String s,int textColor){
        textPaint.setColor(textColor);
        canvas.drawText(s,textStart,getWordCenterPosY(),textPaint);
    }

    private void drawBackground(Canvas canvas){
        backgroundPaint.setColor(getOffsetBackgroundColor());
        canvas.drawRect(getOffsetRect(),backgroundPaint);
        backgroundPaint.setColor(getDataBackgroundColor());
        canvas.drawRect(getDataAndCharsRect(),backgroundPaint);
    }

    private void drawSelectedRange(Canvas canvas){
        if (!isDataSelected())
            return;
        final int start = mDataSelectedRange.getLower();
        final int end = mDataSelectedRange.getUpper();

        if (start == end)
            return;

        float dataBaseX = getWordWith(9.5f);
        float charBaseX = getWordWith(33.5f);
        RectF selectedDataRect;
        RectF selectCharRect;

        int dataTextStartIndex = dataIndex2TextIndex(start);

        int charStartIndex = start/2;

        int viewHeight = getHeight();

        if (end - start == 1){
            selectedDataRect = new RectF(
                    dataBaseX + getWordWith(dataTextStartIndex),
                    0,
                    dataBaseX + getWordWith(dataTextStartIndex+1),
                    viewHeight
            );
            selectCharRect = new RectF(
                    charBaseX + getWordWith(charStartIndex),
                    0,
                    charBaseX + getWordWith(charStartIndex+1),
                    viewHeight
            );
        }
        else {
            int endIndex = dataIndex2TextIndex(end);
            if (isSpaceIndex(endIndex))
                endIndex --;
            selectedDataRect = new RectF(
                    dataBaseX + getWordWith(dataTextStartIndex),
                    0,
                    dataBaseX + getWordWith(endIndex),
                    viewHeight
            );
            selectCharRect = new RectF(
                    charBaseX + getWordWith(charStartIndex),
                    0,
                    charBaseX + getWordWith(end/2),
                    viewHeight
            );
        }


        backgroundPaint.setColor(selectedBackgroundColor);
        canvas.drawRect(selectedDataRect,backgroundPaint);

        backgroundPaint.setColor(selectedBackgroundColor2);
        canvas.drawRect(selectCharRect,backgroundPaint);

    }

    private void drawLine(Canvas canvas){
        float wordWidth9 = getWordWith(9);
        float wordWidth29 = getWordWith(21);
        float wordWidth33 = getWordWith(33);

        linePaint.setStrokeWidth(dp2px(0.6f));
        canvas.drawLine(wordWidth9,0,wordWidth9,getHeight(),linePaint);
        canvas.drawLine(wordWidth29,0,wordWidth29,getHeight(),linePaint);

        linePaint.setStrokeWidth(dp2px(2f));
        canvas.drawLine(wordWidth33,0,wordWidth33,getHeight(),linePaint);
    }

    private int getOffsetBackgroundColor(){
        if (offset % 16 == 0)
            return offsetBackgroundColor1;
        return offsetBackgroundColor2;
    }

    private int getDataBackgroundColor(){
        if (offset % 16 == 0)
            return dataBackgroundColor1;
        return dataBackgroundColor2;
    }

    /**
     * 绘制字符串列表
     * @param strings 字符串列表
     * @param interval 间隔
     * @param empty 空
     * @param x x
     */
    private void drawStrings(Canvas canvas,String[] strings,String interval,String empty,float x,int textStart){
        StringBuilder builder = new StringBuilder();
        for (int i=0;i<8;i++){
            if (interval != null)
                builder.append(interval);

            if (i < strings.length && strings[i] != null){
                builder.append(strings[i]);
            }
            else {
                builder.append(empty);
            }
        }
        textPaint.setColor(dataTextColor);
        canvas.drawText(builder,textStart,builder.length(), x,getWordCenterPosY(), textPaint);
    }

    public void setDataSelectedRange(int start,int end){
        setDataSelectedRange(start, end, false);
    }

    private void setDataSelectedRange(int start,int end,boolean fromUser) {
        end = Math.min(end, dataMaxRange);
        if (end < start) {
            start = end-1;
        }
        if (mDataSelectedRange != null){
            int oldStart = mDataSelectedRange.getLower();
            int oldEnd = mDataSelectedRange.getUpper();
            if (start == oldStart && end == oldEnd)
                return;
        }
        mDataSelectedRange = new Range<>(start,end);
        onSelectedChanged(start,end,fromUser);
        invalidate();
    }

    public void clearDataSelect(){
        mDataSelectedRange = new Range<>(-1,-1);
        onSelectedChanged(-1,-1,false);
        invalidate();
    }

    private void onSelectedChanged(int start,int end,boolean fromUser){
//        if (onSelectedChangeListener == null)
//            return;
//        onSelectedChangeListener.onSelectedChange(this,start,end,fromUser);
    }

    private boolean isDataSelected(){
        int start = mDataSelectedRange.getLower();
        int end = mDataSelectedRange.getUpper();
        return start != end;
    }

    public Range<Integer> getDataSelectedRange() {
        return mDataSelectedRange;
    }

    //用于判断长按的倒计时
    private CountDownTimer longTouchCountDownTimer = null;
    private boolean isLongTouching = false;
    private boolean isTouchMove = false;

    private float mDownX,mDownY;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isTouchMove = false;
                mDownX = event.getX();
                mDownY = event.getY();
                setTouchDownRangeByEvent(event);
                startLongTouchCountDownTimer();
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = Math.abs(event.getX() - mDownX);
                float dy = Math.abs(event.getY() - mDownY);
                if (dy > getHeight()/2f || dx > getWordWith(1)){
                    isTouchMove = true;
                }
            }break;
            case MotionEvent.ACTION_UP:
                if (!isLongTouching && !isTouchMove){
                    onClick();
                }
            case MotionEvent.ACTION_CANCEL:
                isTouchMove = false;
                cancelLongTouchCountDownTimer();
                break;
        }

        return true;
    }

    //触摸按下的类型
    private TouchDownType touchDownType = TouchDownType.NONE;
    //触摸到的半字节数据下标
    private int touchDataIndex = 0;

    public void setTouchDownRangeByEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        RectF dataTextRect = getDataTextRect();

        float wordWidth = getWordWith();
        float basePosX = dataTextRect.left;

        if (dataTextRect.contains(x,y)){
            touchDownType = TouchDownType.DATA;
            x -= basePosX;
            int wordIndex = (int) (x / wordWidth);
            touchDataIndex = textIndex2DataIndex(wordIndex);

        }else if (x < basePosX){
            touchDownType = TouchDownType.OFFSET;
        }
        else {
            touchDownType = TouchDownType.CHARS;
        }
    }

    private void onClick(){
        switch (touchDownType){
            case OFFSET:
                setDataSelectedRange(0,1,true);
                break;
            case DATA:{
                setDataSelectedRange(touchDataIndex,touchDataIndex+1,true);
//                LOG.e("HPT","选中数据"+touchDataIndex+"-"+(touchDataIndex+1));
            }break;
            case CHARS:{
                setDataSelectedRange(15,16,true);
            }break;
        }

        if (onDataClickListener != null)
            onDataClickListener.onClick(this);
    }

    private void onLongClick(){
        if (touchDownType == TouchDownType.DATA){
            VibratorUtils.vibrate(context,80);

            int start = (touchDataIndex / 2) * 2;
            setDataSelectedRange(start,start+2,true);
//            LOG.e("HPT","选中数据"+start+"-"+(start+2));


            if (onDataLongClickListener != null)
                onDataLongClickListener.onClick(this);
        }
    }

    private void startLongTouchCountDownTimer(){
        if (isLongTouching)
            return;
        cancelLongTouchCountDownTimer();
        longTouchCountDownTimer = new CountDownTimer(800,100) {
            @Override
            public void onTick(long millisUntilFinished) { }
            @Override
            public void onFinish() {
                isLongTouching = true;
                post(()-> onLongClick());
            }
        };
        longTouchCountDownTimer.start();
    }

    private void cancelLongTouchCountDownTimer(){
        isLongTouching = false;
        if (longTouchCountDownTimer != null){
            longTouchCountDownTimer.cancel();
            longTouchCountDownTimer = null;
        }
    }

    public void setOnDataClickListener(OnClickListener onDataClickListener) {
        this.onDataClickListener = onDataClickListener;
    }

    public void setOnDataLongClickListener(OnClickListener onDataLongClickListener) {
        this.onDataLongClickListener = onDataLongClickListener;
    }

    private enum TouchDownType {
        NONE,
        OFFSET,
        DATA,
        CHARS
    }

}
