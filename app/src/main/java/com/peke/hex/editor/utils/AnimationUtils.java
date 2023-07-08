package com.peke.hex.editor.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;

public class AnimationUtils {

    public static ObjectAnimator alpha(View view, long duration, float from, float to, boolean start){
        return alpha(view, duration, from, to, start,false);
    }

    public static ObjectAnimator alpha(View view,long duration,float from,float to,boolean start,boolean autoVisibility){
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, from, to);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, alpha);
        animator.setDuration(duration);

        if (autoVisibility){
            if (from == 0)
                view.setVisibility(View.VISIBLE);

            if (to == 0){
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }

        if (start)
            animator.start();

        return animator;
    }


}
