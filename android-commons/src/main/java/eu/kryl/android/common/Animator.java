
/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.common;

import android.content.Context;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

/**
 * Basic class to implement various animations. The class simply animates a value from "start
 * value" to "final value" for the specified duration time
 */
@SuppressWarnings("WeakerAccess")
public class Animator {

    private static final int DEFAULT_DURATION = 250;

    protected float mStartValue;

    protected float mFinalValue;

    protected float mCurrValue;

    protected float mDeltaValue;

    protected long mStartTime;

    protected int mDuration;

    protected float mDurationReciprocal;

    protected boolean mFinished;

    protected Interpolator mInterpolator;

    /** Listener to be notified when the animation starts or ends */
    protected Listener mListener;

    /**
     * Constructor with the default duration and interpolator.
     */
    public Animator(Context context) {
        this(context, null);
    }

    /**
     * Constructor with the specified interpolator. If the interpolator is null, the default
     * (linear) interpolator will be used.
     */
    public Animator(Context context, Interpolator interpolator) {
        mFinished = true;
        mInterpolator = interpolator;
    }

    /**
     * Returns whether the animator is finished
     * 
     * @return True if the animator has finished, false otherwise.
     */
    public final boolean isFinished() {
        return mFinished;
    }

    /**
     * Force the finished field to a particular value.
     * 
     * @param finished The new finished value.
     */
    public final void forceFinished(boolean finished, boolean notifyListener) {
        mFinished = finished;
        if (notifyListener && finished && mListener != null) {
            mListener.onAnimatorEnd(this);
        }
    }

    /**
     * Force the finished field to a particular value.
     * 
     * @param finished The new finished value.
     */
    public final void forceFinished(boolean finished) {
        forceFinished(finished, true);
    }

    /**
     * Returns how long the animation event will take, in milliseconds.
     * 
     * @return The duration of the animation in milliseconds.
     */
    public final int getDuration() {
        return mDuration;
    }

    /**
     * @return current value
     */
    public final float getCurrValue() {
        return mCurrValue;
    }

    /**
     * @return start value
     */
    public final float getStartValue() {
        return mStartValue;
    }

    /**
     * @return final value
     */
    public final float getFinalValue() {
        return mFinalValue;
    }

    /**
     * Call this when you want to know the new value. If it returns true, the animation is not yet
     * finished. Current value will be altered to provide the new value.
     */
    public boolean computeValue() {
        if (mFinished) {
            return false;
        }

        int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);

        if (timePassed < mDuration) {
            float x = (float) timePassed * mDurationReciprocal;

            if (mInterpolator != null) {
                x = mInterpolator.getInterpolation(x);
            }

            mCurrValue = mStartValue + x * mDeltaValue;
        } else {
            mCurrValue = mFinalValue;
            mFinished = true;
            if (mListener != null) {
                mListener.onAnimatorEnd(this);
            }
        }
        return true;
    }

    /**
     * Start animation by providing a starting value and the value delta. The animation will use the
     * default value of {@link #DEFAULT_DURATION} milliseconds for the duration.
     * 
     * @param startValue Start value
     * @param deltaValue Delta value
     */
    public void startAnimation(float startValue, float deltaValue) {
        startAnimation(startValue, deltaValue, DEFAULT_DURATION);
    }

    /**
     * Start animation by providing a starting value and the value delta.
     * 
     * @param startValue Start value
     * @param deltaValue Delta value
     * @param duration Duration of the animation in milliseconds.
     */
    public void startAnimation(float startValue, float deltaValue, int duration) {
        mFinished = false;
        mDuration = duration;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mStartValue = startValue;
        mFinalValue = startValue + deltaValue;
        mDeltaValue = deltaValue;
        mDurationReciprocal = 1.0f / (float) mDuration;
        if (mListener != null) {
            mListener.onAnimatorStart(this);
        }

    }

    /**
     * Stops the animation. Contrary to {@link #forceFinished(boolean)}, aborting the animating
     * cause the animator to set to the final value
     * 
     * @see #forceFinished(boolean)
     */
    public void abortAnimation() {
        mCurrValue = mFinalValue;
        mFinished = true;
        if (mListener != null) {
            mListener.onAnimatorEnd(this);
        }
    }

    public void pauseAnimation() {
        float v = mCurrValue;
        abortAnimation();
        mCurrValue = v;
    }

    /**
     * Sets new the final value for this animator and pauses it too, call continueAnimation to resume
     * 
     * @param newValue The new value to reach.
     * @see #extendDuration(int)
     */
    public void setFinalValueAndPause(float newValue) {
        mFinalValue = newValue;
        mDeltaValue = mFinalValue - mStartValue;
        mFinished = true;
        if (mListener != null) {
            mListener.onAnimatorEnd(this);
        }
    }


    /**
     * Extend the animation value. This allows a running animation to animate further and longer,
     * when used with {@link #setFinalValue(float)}.
     * 
     * @param extend Additional time to animate in milliseconds.
     * @see #setFinalValue(float)
     */
    public void extendDuration(int extend) {
        int passed = timePassed();
        mDuration = passed + extend;
        mDurationReciprocal = 1.0f / (float) mDuration;
        mFinished = false;
    }

    /**
     * Returns the time elapsed since the beginning of the animation.
     * 
     * @return The elapsed time in milliseconds.
     */
    public int timePassed() {
        return (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);
    }

    /**
     * Sets the final value for this animator
     * 
     * @param newValue The new value to reach.
     * @see #extendDuration(int)
     */
    public void setFinalValue(float newValue) {
        mFinalValue = newValue;
        mDeltaValue = mFinalValue - mStartValue;
        mFinished = false;
    }

    /**
     * Binds an animator listener to this animator. The animator listener is notified of animation
     * events such as the end of the animation
     * 
     * @param listener the animator listener to be notified
     */
    public void setAnimatorListener(Listener listener) {
        mListener = listener;
    }

    /**
     * <p>
     * An animator listener receives notifications from an animator. Notifications indicate animator
     * related events, such as the end or the repetition of the animator.
     * </p>
     */
    public interface Listener {
        /**
         * Notifies the start of the animator.
         * 
         * @param animator The started animator.
         */
        void onAnimatorStart(Animator animator);

        /**
         * Notifies the end of the animator. This callback is not invoked for animators with repeat
         * count set to INFINITE.
         * 
         * @param animator The animator which reached its end.
         */
        void onAnimatorEnd(Animator animator);

    }

}
