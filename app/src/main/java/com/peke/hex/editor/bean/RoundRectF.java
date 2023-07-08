package com.peke.hex.editor.bean;

import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RoundRectF {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public float topLeftRadius;
    public float topRightRadius;
    public float bottomLeftRadius;
    public float bottomRightRadius;

    private static final RectF sRectF = new RectF(0,0,200,200);

    public RoundRectF(){
        this(null);
    }

    public RoundRectF(@Nullable RectF rectF){
        this(rectF,0);
    }

    public RoundRectF(@Nullable RectF rectF, float radius){
        this(rectF,radius,radius,radius,radius);
    }

    public RoundRectF(@Nullable RectF rectF, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius){
        if (rectF != null){
            this.left = rectF.left;
            this.top = rectF.top;
            this.right = rectF.right;
            this.bottom = rectF.bottom;
        }
        else {
            this.left = sRectF.left;
            this.top = sRectF.top;
            this.right = sRectF.right;
            this.bottom = sRectF.bottom;
        }
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
        this.bottomLeftRadius = bottomLeftRadius;
        this.bottomRightRadius = bottomRightRadius;
    }

    public RoundRectF setTopLeftRadius(float topLeftRadius){
        this.topLeftRadius = topLeftRadius;
        return this;
    }

    public RoundRectF setTopRightRadius(float topRightRadius){
        this.topRightRadius = topRightRadius;
        return this;
    }

    public RoundRectF setBottomLeftRadius(float bottomLeftRadius){
        this.bottomLeftRadius = bottomLeftRadius;
        return this;
    }

    public RoundRectF setBottomRightRadius(float bottomRightRadius){
        this.bottomRightRadius = bottomRightRadius;
        return this;
    }

    public RoundRectF setRadius(float radius){
        this.topLeftRadius = radius;
        this.topRightRadius = radius;
        this.bottomLeftRadius = radius;
        this.bottomRightRadius = radius;
        return this;
    }

    public final boolean isEmpty() {
        return left >= right || top >= bottom;
    }

    public final float width() {
        return right - left;
    }

    public final float height() {
        return bottom - top;
    }

    public final float centerX() {
        return (left + right) * 0.5f;
    }

    public final float centerY() {
        return (top + bottom) * 0.5f;
    }

    public void setEmpty() {
        left = right = top = bottom = 0;
    }

    public void offset(float dx, float dy) {
        left    += dx;
        top     += dy;
        right   += dx;
        bottom  += dy;
    }

    public void offsetTo(float newLeft, float newTop) {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public boolean contains(float x, float y) {
        return left < right && top < bottom  // check for empty first
                && x >= left && x < right && y >= top && y < bottom;
    }

    public boolean contains(float left, float top, float right, float bottom) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && this.left <= left && this.top <= top
                && this.right >= right && this.bottom >= bottom;
    }

    public boolean contains(@NonNull RectF r) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && left <= r.left && top <= r.top
                && right >= r.right && bottom >= r.bottom;
    }

    public GradientDrawable toDrawable(int color){
        float[] outerRadii = new float[]{topLeftRadius, topLeftRadius, topRightRadius, topRightRadius,
                bottomRightRadius, bottomRightRadius, bottomLeftRadius, bottomLeftRadius};
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(outerRadii);
        drawable.setColor(color);
        drawable.setBounds((int)left,(int)top,(int)right,(int)bottom);
        return drawable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoundRectF r = (RoundRectF) o;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }

    @Override
    public int hashCode() {
        int result = (left != 0f ? Float.floatToIntBits(left) : 0);
        result = 31 * result + (top != 0f ? Float.floatToIntBits(top) : 0);
        result = 31 * result + (right != 0f ? Float.floatToIntBits(right) : 0);
        result = 31 * result + (bottom != 0f ? Float.floatToIntBits(bottom) : 0);
        return result;
    }


    @NonNull
    @Override
    public String toString() {
        return "RoundRectF{" +
                "RectF=("+left+","+top+","+right+","+bottom+"),"+
                "Radius=("+topLeftRadius+","+topRightRadius+","+bottomLeftRadius+","+bottomRightRadius+")"+
                '}';
    }
}
