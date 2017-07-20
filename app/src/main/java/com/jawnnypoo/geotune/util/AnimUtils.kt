package com.jawnnypoo.geotune.util

import android.animation.Animator
import android.annotation.TargetApi
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator

/**
 * Animation stuff
 */
object AnimUtils {

    private val mAccelerateInterpolator = AccelerateInterpolator()

    @TargetApi(21)
    fun circleReveal(v: View, centerX: Int, centerY: Int,
                     startRadius: Float, endRadius: Float, duration: Int): Animator {
        val anim = ViewAnimationUtils.createCircularReveal(v, centerX, centerY, startRadius, endRadius)
        anim.duration = duration.toLong()
        anim.interpolator = mAccelerateInterpolator
        anim.start()
        return anim
    }
}
