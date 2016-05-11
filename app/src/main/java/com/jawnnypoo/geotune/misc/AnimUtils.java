package com.jawnnypoo.geotune.misc;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;

/**
 * Animation stuff
 */
public class AnimUtils {

    private static final AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();

    @TargetApi(21)
    public static Animator circleReveal(View v, int centerX, int centerY,
                                        float startRadius, float endRadius, int duration) {
        Animator anim = ViewAnimationUtils.createCircularReveal(v, centerX, centerY, startRadius, endRadius);
        anim.setDuration(duration);
        anim.setInterpolator(mAccelerateInterpolator);
        anim.start();
        return anim;
    }
}
