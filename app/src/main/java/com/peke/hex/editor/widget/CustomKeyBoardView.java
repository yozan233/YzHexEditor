package com.peke.hex.editor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.peke.hex.editor.R;
import com.peke.hex.editor.bean.RoundRectF;
import com.peke.hex.editor.utils.DisplayUtils;
import com.peke.hex.editor.utils.VibratorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CustomKeyBoardView extends View {

    protected Context mContext;
    private Paint mTextPaint;
    private int mItemMargin;

    //自动连续点击定时器
    private Timer autoContinuousClickTimer;

    private CountDownTimer longTouchCountDownTimer = null;
    private boolean isLongTouching = false;
    private boolean isTouchMove = false;
    private boolean isTouchDown = false;
    private InputKeyConfig curTouchDownConfig = null;

    private OnInputListener onInputListener;

    public CustomKeyBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init(){
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.parseColor("#424242"));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(DisplayUtils.dp2px(mContext,18));
        mTextPaint.setTypeface(Typeface.MONOSPACE);

        adjustTextSize();

        mItemMargin = DisplayUtils.dp2px(mContext,5);
    }

    private void adjustTextSize() {
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        float btnWidth = screenWidth / 5f - DisplayUtils.dp2px(mContext,16);

        float trySize = mTextPaint.getTextSize();
        String text = "确定";
        while (mTextPaint.measureText(text) > btnWidth) {
            trySize--;
            mTextPaint.setTextSize(trySize);
        }

    }

    public int getItemMargin() {
        return mItemMargin;
    }

    public float getItemWidth(){
        int columns = getInputKeyConfigs()[0].length;
        float viewWidth = getWidth() - mItemMargin * (columns+1);
        return viewWidth / columns;
    }

    public float getItemHeight(){
        return DisplayUtils.dp2px(mContext,42);
    }

    public Paint getTextPaint() {
        return mTextPaint;
    }

    protected abstract InputKeyConfig[][] getInputKeyConfigs();

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public final boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                isTouchMove = false;
                isTouchDown = true;
                InputKeyConfig keyConfig = getKeyTextByXY(event.getX(), event.getY());
                if (keyConfig != null){
                    setCurTouchDownConfig(keyConfig);
                    startLongTouchCountDownTimer();
                }
            }break;
            case MotionEvent.ACTION_MOVE:
                if (curTouchDownConfig != null){
                    RectF curTouchDownRect = curTouchDownConfig.rect;
                    if (!curTouchDownRect.contains(event.getX(), event.getY())){
                        isTouchMove = true;
                        setCurTouchDownConfig(null);
                        cancelLongTouchCountDownTimer();
                        cancelAutoContinuousClick();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isTouchMove){
                    _onClick(curTouchDownConfig);
                }
            case MotionEvent.ACTION_CANCEL:
                isTouchMove = false;
                isTouchDown = false;
                setCurTouchDownConfig(null);
                cancelLongTouchCountDownTimer();
                cancelAutoContinuousClick();
                break;
        }
        return true;
    }

    private void startLongTouchCountDownTimer(){
        if (curTouchDownConfig == null || !curTouchDownConfig.longTouchEnable)
            return;
        if (isLongTouching)
            return;
        cancelLongTouchCountDownTimer();
        longTouchCountDownTimer = new CountDownTimer(500,100) {
            @Override
            public void onTick(long millisUntilFinished) { }
            @Override
            public void onFinish() {
                isLongTouching = true;
                post(()-> _onLongClick());
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

    private void _onClick(InputKeyConfig keyConfig){
        if (keyConfig == null)
            return;
        if (!isItemEnable(keyConfig))
            return;

        if (onInputListener != null)
            onInputListener.onInput(keyConfig.key);
    }

    private void _onLongClick(){
        if (curTouchDownConfig == null)
            return;
        if (!isItemEnable(curTouchDownConfig))
            return;
        VibratorUtils.vibrate(mContext,50);
        startAutoContinuousClick();
    }

    /**
     * 长按时自动连续点击
     */
    private void startAutoContinuousClick(){
        cancelAutoContinuousClick();
        autoContinuousClickTimer = new Timer();
        autoContinuousClickTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isTouchDown){
                    autoContinuousClickTimer.cancel();
                    return;
                }
                post(()-> _onClick(curTouchDownConfig));
            }
        },100,100);
    }

    private void cancelAutoContinuousClick(){
        if (autoContinuousClickTimer != null){
            autoContinuousClickTimer.cancel();
            autoContinuousClickTimer = null;
        }
    }

    protected boolean isItemEnable(InputKeyConfig config){
        return true;
    }

    private InputKeyConfig getKeyTextByXY(float x, float y){
        InputKeyConfig[][] inputKeyConfigs = getInputKeyConfigs();
        for (InputKeyConfig[] inputKeyConfig:inputKeyConfigs){
            for (InputKeyConfig config:inputKeyConfig){
                if(config.rect == null)
                    continue;
                if (config.rect.contains(x, y))
                    return config;
            }
        }
        return null;
    }

    private void setCurTouchDownConfig(InputKeyConfig config){
        if (curTouchDownConfig != null && curTouchDownConfig.equals(config))
            return;
        curTouchDownConfig = config;
        invalidate();
    }

    public boolean isKeyTouchDown(String name){
        if (curTouchDownConfig == null)
            return false;
        return curTouchDownConfig.name.equalsIgnoreCase(name);
    }

    protected void drawItemBackground(Canvas canvas, int resId, RectF rect){
        Drawable drawable = ContextCompat.getDrawable(mContext,resId);
        drawItemBackground(canvas, drawable, rect);
    }

    protected void drawItemBackground(Canvas canvas, Drawable drawable, RectF rect){
        if (drawable == null)
            return;
        drawable.setBounds(RectF2Int(rect));
        drawable.draw(canvas);
    }

    protected Bitmap createIcon(int resId, float scale){
        return createIcon(resId,scale,0);
    }

    protected Bitmap createIcon(int resId, float scale,@ColorRes int tintRes){
        Paint textPaint = getTextPaint();
        float textSize = textPaint.getTextSize() * scale;
        int size = (int) textSize;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable drawable = ContextCompat.getDrawable(mContext,resId);
        if (drawable != null){
            drawable.setBounds(0, 0, size, size);
            if (tintRes != 0) {
                drawable.setTint(ContextCompat.getColor(mContext, tintRes));
            }
            drawable.draw(canvas);
        }
        return bitmap;
    }

    private Rect RectF2Int(RectF rectF){
        return new Rect(
                (int) rectF.left,
                (int) rectF.top,
                (int) rectF.right,
                (int) rectF.bottom
        );
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dp240 = DisplayUtils.dp2px(mContext,240);
        int rows = getInputKeyConfigs().length;
        int margin = mItemMargin * (rows + 1);
        float itemHeight = getItemHeight();
        int height = (int) (itemHeight * rows + margin);
        setMeasuredDimension(DisplayUtils.getScreenWidth(mContext),Math.max(height,dp240));
    }

    public static int getKeyBoardHeight(Context context){
        return DisplayUtils.dp2px(context,240) + DisplayUtils.sysDp2px(context,0.6f);
    }

    public void setOnInputListener(OnInputListener onInputListener) {
        this.onInputListener = onInputListener;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        throw new RuntimeException("please use setOnInputListener");
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        throw new RuntimeException("please use setOnInputListener");
    }

    protected ButtonBackgroundBuilder createButtonBackground(@ColorRes int colorRes){
        return new ButtonBackgroundBuilder(mContext.getResources().getColor(colorRes));
    }

    public enum InputKey {
        /** 数字0 */
        KEY_0(0),
        /** 数字1 */
        KEY_1(1),
        /** 数字2 */
        KEY_2(2),
        /** 数字3 */
        KEY_3(3),
        /** 数字4 */
        KEY_4(4),
        /** 数字5 */
        KEY_5(5),
        /** 数字6 */
        KEY_6(6),
        /** 数字7 */
        KEY_7(7),
        /** 数字8 */
        KEY_8(8),
        /** 数字9 */
        KEY_9(9),
        /** 数字A */
        KEY_A(0xA),
        /** 数字B */
        KEY_B(0xB),
        /** 数字C */
        KEY_C(0xC),
        /** 数字D */
        KEY_D(0xD),
        /** 数字E */
        KEY_E(0xE),
        /** 数字F */
        KEY_F(0xF),
        /** +/-符号 */
        KEY_SIGN(-1),
        /** 开始 */
        KEY_START,
        /** 末尾 */
        KEY_END,
        /** 选择 */
        KEY_SELECT,
        /** 全选 */
        KEY_SELECT_ALL,
        /** 方向左 */
        KEY_LEFT,
        /** 方向右 */
        KEY_RIGHT,
        /** 方向上 */
        KEY_UP,
        /** 方向下 */
        KEY_DOWN,
        /** 输入 */
        KEY_ENTRY,

        ;
        public final int value;

        InputKey(){
            this(Integer.MIN_VALUE);
        }
        InputKey(int value){
            this.value = value;
        }
        
        public boolean isNumber(){
            return value >= 0 && value <= 9;
        }

        public boolean isBinNumber(){
            return value >= 0 && value <= 1;
        }
        
        public boolean isHexNumber(){
            return value >= 0 && value <= 0xF;
        }
        
    }

    protected static class InputKeyConfig {
        public final String name;
        public final InputKey key;
        public RectF rect;
        public boolean longTouchEnable;

        public InputKeyConfig(String name, InputKey key){
            this(name,key,true);
        }

        public InputKeyConfig(String name, InputKey key,boolean longTouchEnable) {
            this.name = name;
            this.key = key;
            this.longTouchEnable = longTouchEnable;
        }

        public boolean isNumber(){
            return key.value >= 0 && key.value <= 0xF;
        }

    }

    protected static class ButtonBackgroundBuilder {
        private static final Map<String,Drawable> sDrawableCache = new HashMap<>();
        RoundRectF mRoundRectF = new RoundRectF();
        private final int color;

        public ButtonBackgroundBuilder(int color){
            this.color = color;
        }

        public ButtonBackgroundBuilder topLeftRadius(float topLeftRadius){
            mRoundRectF.topLeftRadius = topLeftRadius;
            return this;
        }

        public ButtonBackgroundBuilder topRightRadius(float topRightRadius){
            mRoundRectF.topRightRadius = topRightRadius;
            return this;
        }

        public ButtonBackgroundBuilder bottomLeftRadius(float bottomLeftRadius){
            mRoundRectF.bottomLeftRadius = bottomLeftRadius;
            return this;
        }

        public ButtonBackgroundBuilder bottomRightRadius(float bottomRightRadius){
            mRoundRectF.bottomRightRadius = bottomRightRadius;
            return this;
        }

        public ButtonBackgroundBuilder radius(float radius){
            mRoundRectF.topLeftRadius = radius;
            mRoundRectF.topRightRadius = radius;
            mRoundRectF.bottomLeftRadius = radius;
            mRoundRectF.bottomRightRadius = radius;
            return this;
        }

        public Drawable build(){
            String key = mRoundRectF.toString() + color;
            Drawable drawable = sDrawableCache.get(key);
            if (drawable == null){
                drawable = mRoundRectF.toDrawable(color);
                sDrawableCache.put(key,drawable);
            }
            return drawable;
        }

    }

    public interface OnInputListener {
        void onInput(InputKey inputKey);
    }


}
