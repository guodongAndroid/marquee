package com.guodongandroid.marquee.androidx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Choreographer;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Created by guodongAndroid on 2021/6/9.
 */
public class VerticalMarqueeTextView extends AppCompatTextView {

    private static final int DEFAULT_BG_COLOR = Color.parseColor("#FFEFEFEF");

    private Marquee mMarquee;
    private boolean mRestartMarquee;
    private boolean isMarquee;

    public VerticalMarqueeTextView(Context context) {
        this(context, null);
    }

    public VerticalMarqueeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalMarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (getHeight() > 0) {
            mRestartMarquee = true;
        }
    }

    private void restartMarqueeIfNeeded() {
        if (mRestartMarquee) {
            mRestartMarquee = false;
            startMarquee();
        }
    }

    public void setMarquee(boolean marquee) {
        boolean wasStart = isMarquee();

        isMarquee = marquee;

        if (wasStart != marquee) {
            if (marquee) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }

    public boolean isMarquee() {
        return isMarquee;
    }

    private void stopMarquee() {
        setVerticalFadingEdgeEnabled(false);
        requestLayout();
        invalidate();

        if (mMarquee != null && !mMarquee.isStopped()) {
            mMarquee.stop();
        }
    }

    private void startMarquee() {
        if (canMarquee()) {
            setVerticalFadingEdgeEnabled(true);
            if (mMarquee == null) mMarquee = new Marquee(this);
            mMarquee.start(-1);
        }
    }

    private boolean canMarquee() {
        int viewHeight = getHeight() - getCompoundPaddingTop() -
                getCompoundPaddingBottom();
        float textHeight = getLayout().getHeight();
        return (mMarquee == null || mMarquee.isStopped())
                && (isFocused() || isSelected() || isMarquee())
                && viewHeight > 0
                && textHeight > viewHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        restartMarqueeIfNeeded();

        super.onDraw(canvas);

        Drawable background = getBackground();
        if (background != null) {
            background.draw(canvas);
        } else {
            canvas.drawColor(DEFAULT_BG_COLOR);
        }

        canvas.save();

        canvas.translate(0, 0);

        if (mMarquee != null && mMarquee.isRunning()) {
            final float dy = -mMarquee.getScroll();
            canvas.translate(0.0F, dy);
        }

        getLayout().draw(canvas, null, null, 0);

        if (mMarquee != null && mMarquee.shouldDrawGhost()) {
            final float dy = mMarquee.getGhostOffset();
            canvas.translate(0.0F, dy);
            getLayout().draw(canvas, null, null, 0);
        }

        canvas.restore();
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (mMarquee != null && !mMarquee.isStopped()) {
            final Marquee marquee = mMarquee;
            if (marquee.shouldDrawTopFade()) {
                final float scroll = marquee.getScroll();
                return scroll / getVerticalFadingEdgeLength();
            } else {
                return 0.0F;
            }
        }
        return super.getTopFadingEdgeStrength();
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (mMarquee != null && !mMarquee.isStopped()) {
            final Marquee marquee = mMarquee;
            final float maxFadeScroll = marquee.getMaxFadeScroll();
            final float scroll = marquee.getScroll();
            return (maxFadeScroll - scroll) / getVerticalFadingEdgeLength();
        }
        return super.getBottomFadingEdgeStrength();
    }

    private static final class Marquee {
        private static final int MARQUEE_DELAY = 1200;
        private static final int MARQUEE_DP_PER_SECOND = 30;

        private static final byte MARQUEE_STOPPED = 0x0;
        private static final byte MARQUEE_STARTING = 0x1;
        private static final byte MARQUEE_RUNNING = 0x2;

        private static final String METHOD_GET_FRAME_TIME = "getFrameTime";

        private final WeakReference<VerticalMarqueeTextView> mView;
        private final Choreographer mChoreographer;

        private byte mStatus = MARQUEE_STOPPED;
        private final float mPixelsPerSecond;
        private float mMaxScroll;
        private float mMaxFadeScroll;
        private float mGhostStart;
        private float mGhostOffset;
        private float mFadeStop;
        private int mRepeatLimit;

        private float mScroll;
        private long mLastAnimationMs;

        Marquee(VerticalMarqueeTextView v) {
            final float density = v.getContext().getResources().getDisplayMetrics().density;
            mPixelsPerSecond = MARQUEE_DP_PER_SECOND * density;
            mView = new WeakReference<>(v);
            mChoreographer = Choreographer.getInstance();
        }

        private final Choreographer.FrameCallback mTickCallback = frameTimeNanos -> tick();

        private final Choreographer.FrameCallback mStartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                mStatus = MARQUEE_RUNNING;
                mLastAnimationMs = getFrameTime();
                tick();
            }
        };

        @SuppressLint("PrivateApi")
        private long getFrameTime() {
            try {
                Class<? extends Choreographer> clz = mChoreographer.getClass();
                Method getFrameTime = clz.getDeclaredMethod(METHOD_GET_FRAME_TIME);
                getFrameTime.setAccessible(true);
                return (long) getFrameTime.invoke(mChoreographer);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        private final Choreographer.FrameCallback mRestartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (mStatus == MARQUEE_RUNNING) {
                    if (mRepeatLimit >= 0) {
                        mRepeatLimit--;
                    }
                    start(mRepeatLimit);
                }
            }
        };

        void tick() {
            if (mStatus != MARQUEE_RUNNING) {
                return;
            }

            mChoreographer.removeFrameCallback(mTickCallback);

            final VerticalMarqueeTextView textView = mView.get();
            if (textView != null && (textView.isFocused() || textView.isSelected() || textView.isMarquee())) {
                long currentMs = getFrameTime();
                long deltaMs = currentMs - mLastAnimationMs;
                mLastAnimationMs = currentMs;
                float deltaPx = deltaMs / 1000F * mPixelsPerSecond;
                mScroll += deltaPx;
                if (mScroll > mMaxScroll) {
                    mScroll = mMaxScroll;
                    mChoreographer.postFrameCallbackDelayed(mRestartCallback, MARQUEE_DELAY);
                } else {
                    mChoreographer.postFrameCallback(mTickCallback);
                }
                textView.invalidate();
            }
        }

        void stop() {
            mStatus = MARQUEE_STOPPED;
            mChoreographer.removeFrameCallback(mStartCallback);
            mChoreographer.removeFrameCallback(mRestartCallback);
            mChoreographer.removeFrameCallback(mTickCallback);
            resetScroll();
        }

        private void resetScroll() {
            mScroll = 0.0F;
            final VerticalMarqueeTextView textView = mView.get();
            if (textView != null) textView.invalidate();
        }

        void start(int repeatLimit) {
            if (repeatLimit == 0) {
                stop();
                return;
            }
            mRepeatLimit = repeatLimit;
            final VerticalMarqueeTextView textView = mView.get();
            if (textView != null && textView.getLayout() != null) {
                mStatus = MARQUEE_STARTING;
                mScroll = 0.0F;
                int viewHeight = textView.getHeight() - textView.getCompoundPaddingTop() -
                        textView.getCompoundPaddingBottom();
                float textHeight = textView.getLayout().getHeight();
                float gap = viewHeight / 3.0F;
                mGhostStart = textHeight - viewHeight + gap;
                mMaxScroll = mGhostStart + viewHeight;
                mGhostOffset = textHeight + gap;
                mFadeStop = textHeight + viewHeight / 6.0F;
                mMaxFadeScroll = mGhostStart + textHeight + textHeight;

                textView.invalidate();
                mChoreographer.postFrameCallback(mStartCallback);
            }
        }

        float getGhostOffset() {
            return mGhostOffset;
        }

        float getScroll() {
            return mScroll;
        }

        float getMaxFadeScroll() {
            return mMaxFadeScroll;
        }

        boolean shouldDrawTopFade() {
            return mScroll <= mFadeStop;
        }

        boolean shouldDrawGhost() {
            return mStatus == MARQUEE_RUNNING && mScroll > mGhostStart;
        }

        boolean isRunning() {
            return mStatus == MARQUEE_RUNNING;
        }

        boolean isStopped() {
            return mStatus == MARQUEE_STOPPED;
        }
    }
}
