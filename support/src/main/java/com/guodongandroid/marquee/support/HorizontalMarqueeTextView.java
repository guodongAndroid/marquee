package com.guodongandroid.marquee.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.Choreographer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Created by guodongAndroid on 2021/6/9.
 */
public class HorizontalMarqueeTextView extends AppCompatTextView {

    private static final int DEFAULT_BG_COLOR = Color.parseColor("#FFEFEFEF");

    private Marquee mMarquee;
    private boolean mRestartMarquee;
    private boolean isMarquee;

    public HorizontalMarqueeTextView(Context context) {
        this(context, null);
    }

    public HorizontalMarqueeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalMarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (getWidth() > 0) {
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
        setHorizontalFadingEdgeEnabled(false);
        requestLayout();
        invalidate();

        if (mMarquee != null && !mMarquee.isStopped()) {
            mMarquee.stop();
        }
    }

    private void startMarquee() {
        if (canMarquee()) {
            setHorizontalFadingEdgeEnabled(true);
            if (mMarquee == null) mMarquee = new Marquee(this);
            mMarquee.start(-1);
        }
    }

    private boolean canMarquee() {
        int viewWidth = getWidth() - getCompoundPaddingLeft() -
                getCompoundPaddingRight();
        float lineWidth = getLayout().getLineWidth(0);
        return (mMarquee == null || mMarquee.isStopped())
                && (isFocused() || isSelected() || isMarquee())
                && viewWidth > 0
                && lineWidth > viewWidth;
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
            final float dx = -mMarquee.getScroll();
            canvas.translate(dx, 0.0F);
        }

        getLayout().draw(canvas, null, null, 0);

        if (mMarquee != null && mMarquee.shouldDrawGhost()) {
            final float dx = mMarquee.getGhostOffset();
            canvas.translate(dx, 0.0F);
            getLayout().draw(canvas, null, null, 0);
        }

        canvas.restore();
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (mMarquee != null && !mMarquee.isStopped()) {
            final Marquee marquee = mMarquee;
            if (marquee.shouldDrawLeftFade()) {
                final float scroll = marquee.getScroll();
                return scroll / getHorizontalFadingEdgeLength();
            } else {
                return 0.0F;
            }
        }
        return super.getLeftFadingEdgeStrength();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (mMarquee != null && !mMarquee.isStopped()) {
            final Marquee marquee = mMarquee;
            final float maxFadeScroll = marquee.getMaxFadeScroll();
            final float scroll = marquee.getScroll();
            return (maxFadeScroll - scroll) / getHorizontalFadingEdgeLength();
        }
        return super.getRightFadingEdgeStrength();
    }

    private static final class Marquee {
        private static final int MARQUEE_DELAY = 1200;
        private static final int MARQUEE_DP_PER_SECOND = 30;

        private static final byte MARQUEE_STOPPED = 0x0;
        private static final byte MARQUEE_STARTING = 0x1;
        private static final byte MARQUEE_RUNNING = 0x2;

        private static final String METHOD_GET_FRAME_TIME = "getFrameTime";

        private final WeakReference<HorizontalMarqueeTextView> mView;
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

        Marquee(HorizontalMarqueeTextView v) {
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

            final HorizontalMarqueeTextView textView = mView.get();
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
            final HorizontalMarqueeTextView textView = mView.get();
            if (textView != null) textView.invalidate();
        }

        void start(int repeatLimit) {
            if (repeatLimit == 0) {
                stop();
                return;
            }
            mRepeatLimit = repeatLimit;
            final HorizontalMarqueeTextView textView = mView.get();
            if (textView != null && textView.getLayout() != null) {
                mStatus = MARQUEE_STARTING;
                mScroll = 0.0F;
                int viewWidth = textView.getWidth() - textView.getCompoundPaddingLeft() -
                        textView.getCompoundPaddingRight();
                float lineWidth = textView.getLayout().getLineWidth(0);
                float gap = viewWidth / 3.0F;
                mGhostStart = lineWidth - viewWidth + gap;
                mMaxScroll = mGhostStart + viewWidth;
                mGhostOffset = lineWidth + gap;
                mFadeStop = lineWidth + viewWidth / 6.0F;
                mMaxFadeScroll = mGhostStart + lineWidth + lineWidth;

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

        boolean shouldDrawLeftFade() {
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
