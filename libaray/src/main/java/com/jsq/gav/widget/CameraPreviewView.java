package com.jsq.gav.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.jsq.gav.LogUtil;
import com.jsq.gav.R;
import com.jsq.gav.gles.GlFilterConfig;
import com.jsq.gav.gles.filter.GlFilter;
import com.jsq.gav.gles.filter.GlLutFilterPager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/4/26
 */

public class CameraPreviewView extends ViewGroup implements TextureView.SurfaceTextureListener {

    private static final int textureViewId = generateViewId();

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private CameraRenderThread mRenderThread;
    private CameraPreviewHandler mPreviewHandler;

    /**
     * 扩展线程，目前干的事情只有截图
     */
    private HandlerThread mExtThread;
    private Handler mExtEventHandler;

    private TextureView mTextureView;

    private List<OnDrawFrameListener> mDrawFrameListeners;
    private boolean mIsFullScreen = false;
    private boolean mIsStartRender = false;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private Camera.PreviewCallback mPreviewCallback;
    private GlFilterConfig mGlFilterConfig;

    private float mBeautyIntensity = -1;

    private List<String> mLookupImagePathList;
    private int mLutFilterIndex = -1;
    private GlLutFilterPager.LookupFilterScroller mFilterScroller;
    private GlLutFilterPager.OnPageChangeListener mImageLookupFilter;

    /**
     * 相机聚焦半径
     */
    private int mFocusingRadius = 50;

    public CameraPreviewView(Context context) {
        this(context, null);
    }

    public CameraPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CameraPreviewView, 0, 0);
        mFocusingRadius = a.getDimensionPixelSize(R.styleable.CameraPreviewView_focusing_radius, mFocusingRadius);
        a.recycle();

        mPreviewHandler = new CameraPreviewHandler();
        mTextureView = new TextureView(getContext());
        mTextureView.setId(textureViewId);
        MaskView maskView = new MaskView(getContext());
        maskView.setOnClickPointListener((x, y) -> {
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mRenderThread.getHandler().sendSetFocusing(mFocusingRadius, x, y);
            }
        });
        addView(maskView);
        mFilterScroller = new GlLutFilterPager.LookupFilterScroller(getContext());
        mExtThread = new HandlerThread("ext");
        mExtThread.start();
        mExtEventHandler = new Handler(mExtThread.getLooper());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        LogUtil.e("height:" + height);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        } else {
            if (mIsFullScreen) {
                float sx = 1f * width / mRatioWidth;
                float sy = 1f * height / mRatioHeight;
                int childWidth;
                int childHeight;
                if (sx < sy) {
                    childWidth = height * mRatioWidth / mRatioHeight;
                    childHeight = height;
                } else {
                    childWidth = width;
                    childHeight = width * mRatioHeight / mRatioWidth;
                }
                LogUtil.e("childWidth:" + childWidth + " childHeight:" + childHeight);
                setMeasuredDimension(width, height);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
            } else {
                int childHeight = width * mRatioHeight / mRatioWidth;
                setMeasuredDimension(width, childHeight);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
            }
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int childLeft = (r - width) >> 1;
            int childTop = (b - height) >> 1;
            child.layout(childLeft, childTop, childLeft + width, childTop + height);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setAspectRatio(int width, int height) {
        int newWidth;
        int newHeight;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            newWidth = height;
            newHeight = width;
        } else {
            newWidth = width;
            newHeight = height;
        }
        if (newWidth != mRatioWidth || newHeight != mRatioHeight) {
            mRatioWidth = newWidth;
            mRatioHeight = newHeight;
            requestLayout();
        }
    }


    public void setLutFilterDataList(List<String> list) {
        mLookupImagePathList = list;
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendSetLookupFilterImageList(list);
        }
    }

    public void setLutFilterIndex(int index) {
        mLutFilterIndex = index;
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendSetLookupFilterIndex(index);
        }
    }

    public void setLutFilterPagerChangeListener(GlLutFilterPager.OnPageChangeListener listener) {
        mImageLookupFilter = listener;
        if (mRenderThread != null) {
            mRenderThread.setLookupOnPageChangeListener(listener);
        }
    }


    public void setBeautyIntensity(float intensity) {
        mBeautyIntensity = intensity;
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendSetBeautyIntensity(intensity);
        }
    }


    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
        if (mRenderThread != null) {
            mRenderThread.setPreviewCallback(mPreviewCallback);
        }
    }

    /**
     * 增加回调
     */
    public void addOnDrawFrameListener(OnDrawFrameListener listener) {
        if (mDrawFrameListeners == null) mDrawFrameListeners = new ArrayList<>();
        mDrawFrameListeners.add(listener);
        if (mRenderThread != null) {
            mRenderThread.addOnDrawFrameListener(listener);
        }
    }

    /**
     * 清理回调
     */
    public void removeOnDrawFrameListener() {
        if (mDrawFrameListeners != null) {
            mDrawFrameListeners.clear();
            mDrawFrameListeners = null;
        }
        if (mRenderThread != null) {
            mRenderThread.removeOnDrawFrameListener();
        }
    }

    public void screenshot(OnScreenshotListener listener) {
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendScreenshot(listener);
        }
    }

    public void setIsFullScreen(boolean isFullScreen) {
        this.mIsFullScreen = isFullScreen;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startRender();
        mFilterScroller.setRenderEventHandler(mRenderThread.getHandler());
        mRenderThread.getHandler().sendSurfaceAvailable(surface, width, height);
        if (mGlFilterConfig != null) {
            mRenderThread.getHandler().sendAddFilter(mGlFilterConfig);
        }
        if (mLookupImagePathList != null) {
            mRenderThread.getHandler().sendSetLookupFilterImageList(mLookupImagePathList);
        }

        if (mImageLookupFilter != null) {
            mRenderThread.setLookupOnPageChangeListener(mImageLookupFilter);
            if (mLutFilterIndex >= 0) {
                mRenderThread.getHandler().sendSetLookupFilterIndex(mLutFilterIndex);
            }
        }
        if (mBeautyIntensity >= 0) {
            mRenderThread.getHandler().sendSetBeautyIntensity(mBeautyIntensity);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        LogUtil.e("onSurfaceTextureSizeChanged() surface = " + surface + ", width = " + width + ", height = " + height + "");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopRender();
        LogUtil.d("onSurfaceTextureDestroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mFilterScroller.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mFilterScroller.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void startRender() {
        if (mRenderThread == null) {
            mRenderThread = new CameraRenderThread(getContext(), mPreviewHandler, mCameraId, mExtEventHandler, mFilterScroller);
            if (mRatioWidth > 0 && mRatioHeight > 0) {
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mRenderThread.setCameraPreviewSize(mRatioWidth, mRatioHeight);
                } else {
                    //noinspection SuspiciousNameCombination
                    mRenderThread.setCameraPreviewSize(mRatioHeight, mRatioWidth);
                }
            }
            if (mDrawFrameListeners != null) {
                mRenderThread.addOnDrawFrameListener(mDrawFrameListeners);
            }
            if (mPreviewCallback != null) {
                mRenderThread.setPreviewCallback(mPreviewCallback);
            }

            mRenderThread.start();
            mRenderThread.await();

            mIsStartRender = true;
        }
    }

    private void stopRender() {
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendQuit();
            try {
                mRenderThread.join();
                mTextureView.setSurfaceTextureListener(null);
                mIsStartRender = false;
                mRenderThread = null;
            } catch (InterruptedException e) {
                // ignore
            }

        }
    }

    public void refresh() {
        if (getChildCount() > 0) {
            removeView(mTextureView);
        }
        if (mTextureView != null) {
            addTextureView();
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    public void onResume() {
        if (findViewById(textureViewId) != null) {
            removeView(mTextureView);
            mTextureView.setSurfaceTextureListener(null);
        }
        addTextureView();
        mTextureView.setSurfaceTextureListener(this);
    }

    public void onPause() {
        mRenderThread.getHandler().sendStopPreview();
    }


    public GlFilterConfig addFilter(GlFilter glFilter) {
        GlFilterConfig config = new GlFilterConfig(glFilter);
        mGlFilterConfig = config;
        if (mRenderThread != null) {
            mRenderThread.getHandler().sendAddFilter(config);
        }
        return config;
    }

    private void addTextureView() {
        if (findViewById(textureViewId) != null) {
            throw new RuntimeException("CameraPreviewView上已经有一个TextureView了");
        }
        addView(mTextureView, 0);
    }

    public void toggleCamera() {
        if (mRenderThread.CameraId() == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mExtEventHandler.removeCallbacksAndMessages(null);
        mExtThread.quitSafely();
        try {
            mExtThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

    }


    public interface OnDrawFrameListener {
        //        void onDrawFrame(int texture, EGLContext eglContext);
        void onDrawFrame(int texture);
    }

    public interface OnScreenshotListener {
        void onScreenshot(Bitmap bitmap);
    }


    static class CameraPreviewHandler extends Handler {

    }


    private static class MaskView extends View {

        private Paint mPaint;
        private Point mPoint;
        private int mTouchSlop;

        private boolean mPrePressed = false;
        private boolean mShowIndicator;

        private float mLastDownX;
        private float mLastDownY;

        private Runnable mCheckForTap = () -> mPrePressed = false;
        private ValueAnimator mIndicatorAnimator;
        private RectF mIndicatorRect;
        private OnClickPointListener mOnClickPointListener;
        private Runnable mHideIndicator = () -> {
            mShowIndicator = false;
            postInvalidate();
        };

        public MaskView(Context context) {
            super(context);
            ViewConfiguration vc = ViewConfiguration.get(getContext());
            mTouchSlop = vc.getScaledTouchSlop();
            mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(5);
            mPoint = new Point();
            mIndicatorAnimator = new ValueAnimator();
            mIndicatorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mIndicatorRect = new RectF();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mShowIndicator) {
                canvas.drawOval(mIndicatorRect, mPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                    mPrePressed = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    mPrePressed = true;
                    mLastDownX = event.getX();
                    mLastDownY = event.getY();
                    postDelayed(mCheckForTap, ViewConfiguration.getTapTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if ((Math.abs(event.getX() - mLastDownX)) > mTouchSlop
                            || (Math.abs(event.getY() - mLastDownY)) > mTouchSlop) {
                        mPrePressed = false;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mPrePressed = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (mPrePressed) {
                        performClick();
                        showIndicatorOnAnimator(((int) mLastDownX), ((int) mLastDownY));
                        if (mOnClickPointListener != null) {
                            mOnClickPointListener.onClickPoint(((int) mLastDownX), ((int) mLastDownY));
                        }
                        mPrePressed = false;
                    }
                    break;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        public void showIndicatorOnAnimator(int x, int y) {
            mIndicatorAnimator.setDuration(300);
            mIndicatorAnimator.setIntValues(160, 140);
            mIndicatorAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                int left = mPoint.x - value;
                int top = mPoint.y - value;
                int right = mPoint.x + value;
                int bottom = mPoint.y + value;
                mIndicatorRect.set(left, top, right, bottom);
                postInvalidate(left, top, right, bottom);
            });
            mIndicatorAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mShowIndicator = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    hideIndicatorDelayed();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mShowIndicator = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mPoint.x = x;
            mPoint.y = y;
            if (mIndicatorAnimator.isRunning()) {
                mIndicatorAnimator.cancel();
            }
            mIndicatorAnimator.start();


        }

        private void hideIndicatorDelayed() {
            postDelayed(mHideIndicator, 300);
        }


        private void setOnClickPointListener(OnClickPointListener onClickPointListener) {
            mOnClickPointListener = onClickPointListener;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            removeCallbacks(mCheckForTap);
            removeCallbacks(mHideIndicator);
        }

    }

    interface OnClickPointListener {
        void onClickPoint(int x, int y);
    }


}
