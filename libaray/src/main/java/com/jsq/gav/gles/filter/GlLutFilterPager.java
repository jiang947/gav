package com.jsq.gav.gles.filter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.opengl.GLES20;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.GlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.List;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;


/**
 * Created by jiang on 2019/4/24
 */

public class GlLutFilterPager extends GlFilter {

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "gl_Position = aPosition;\n" +
            "vTextureCoord = aTextureCoord.xy;\n" +
            "}\n";

    private int maPositionHandle;
    private int maTextureHandle;

    private int msTextureHandle;
    private int mCurrentLutTextureHandle;
    private int mPrevLutTextureHandle;
    private int mNextLutTextureHandle;
    private int muIntensityHandle;
    private int muScrollXHandle;


    private Buffer posCoordsVertices;
    private Buffer texCoordsVertices;

    private int mCurrentTextureId;
    private int mPrevTextureId;
    private int mNextTextureId;
    private int mCurrentIndex = 0;

    private float mIntensity = 0.8f;
    private volatile float mScrollX = 0f;

    private List<String> mImagePathList;


    public GlLutFilterPager(Context context) {
        super(context);
    }

    public void setLookupImageList(List<String> imagePathList) {
        mImagePathList = imagePathList;
    }

    private OnPageChangeListener mOnPageChangeListener;

    private Handler mCallBackHandler;

    @Override
    public void setup() {
        super.setup();
        posCoordsVertices = GlUtil.createFloatBuffer(GlConstants.FULL_RECTANGLE_COORDS);
        texCoordsVertices = GlUtil.createFloatBuffer(GlConstants.UV_COORDS);

        createProgram(VERTEX_SHADER, loadShaderFormAssets("shaders/fragment_image_lookup.glsl"));
        maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        // fragment shader handle
        msTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
        mCurrentLutTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "currentLutTexture");
        mPrevLutTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "prevLutTexture");
        mNextLutTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "nextLutTexture");
        muIntensityHandle = GLES20.glGetUniformLocation(mProgramHandle, "uIntensity");
        muScrollXHandle = GLES20.glGetUniformLocation(mProgramHandle, "uScrollX");

        int[] textures = new int[3];
        GLES20.glGenTextures(3, textures, 0);
        mCurrentTextureId = textures[0];
        mPrevTextureId = textures[1];
        mNextTextureId = textures[2];

        bindingTextures();
    }

    @Override
    public void release() {
        super.release();
        int[] textures = {mCurrentTextureId};
        GLES20.glDeleteTextures(1, textures, 0);
    }


    @Override
    public int onDraw(int textureId) {
        GLES20.glUseProgram(mProgramHandle);

        GLES20.glActiveTexture(GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(msTextureHandle, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GL_TEXTURE_2D, mCurrentTextureId);
        glUniform1i(mCurrentLutTextureHandle, 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GL_TEXTURE_2D, mPrevTextureId);
        glUniform1i(mPrevLutTextureHandle, 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GL_TEXTURE_2D, mNextTextureId);
        glUniform1i(mNextLutTextureHandle, 4);

        glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GL_FLOAT, false, 0, posCoordsVertices);

        glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GL_FLOAT, false, 0, texCoordsVertices);

        glUniform1f(muIntensityHandle, mIntensity);
        glUniform1f(muScrollXHandle, mScrollX);

        // draw
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        // Done -- disable ...
        glDisableVertexAttribArray(maPositionHandle);
        glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        return textureId;
    }

    public void setIntensity(float intensity) {
        mIntensity = intensity;
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener, Handler handler) {
        this.mCallBackHandler = handler;
        mOnPageChangeListener = onPageChangeListener;
    }

    public boolean isEnablePager() {
        return mCallBackHandler != null && mOnPageChangeListener != null;
    }

    public void scrollBy(float percent) {
        mScrollX += percent;
        if (mScrollX > 1) {
            mScrollX = 1;
            mCurrentIndex++;
        }
        if (mScrollX < -1) {
            mScrollX = -1;
            mCurrentIndex--;
        }
    }

    public void scrollTo(float percent) {
        mScrollX = percent;
        if (mScrollX >= 1) {
            mScrollX = 1;
            final int next = mCurrentIndex - 1;
            setCurrentItem(next);
            if (next == mCurrentIndex) {
                dispatchListener();
            }
        }
        if (mScrollX <= -1) {
            mScrollX = -1;
            final int next = mCurrentIndex + 1;
            setCurrentItem(mCurrentIndex + 1);
            if (next == mCurrentIndex) {
                dispatchListener();
            }
        }
    }


    private void dispatchListener() {
        if (mCallBackHandler != null && mOnPageChangeListener != null) {
            mCallBackHandler.post(() -> {
                mOnPageChangeListener.onPageSelected(mCurrentIndex);
            });
        }
    }

    public float getScrollX() {
        return mScrollX;
    }

    public boolean canScrollX(float p) {
        if (p > 0) {
            return mCurrentIndex > 0;
        } else if (p < 0) {
            return mCurrentIndex + 1 < mImagePathList.size();
        }
        return false;
    }

    public void setCurrentItem(int item) {
        if (item < 0 || item >= mImagePathList.size()) {
            return;
        }
        if (mCurrentIndex == item) {
            return;
        }
        mCurrentIndex = item;
        bindingTextures();
    }

    private void bindingTextures() {
        loadImageAndBindingTexture(mCurrentTextureId, mImagePathList.get(mCurrentIndex));
        if (mCurrentIndex - 1 >= 0) {
            loadImageAndBindingTexture(mPrevTextureId, mImagePathList.get(mCurrentIndex - 1));
        }
        if (mCurrentIndex + 1 < mImagePathList.size()) {
            loadImageAndBindingTexture(mNextTextureId, mImagePathList.get(mCurrentIndex + 1));
        }
        mScrollX = 0f;
    }

    private void loadImageAndBindingTexture(int textureId, String imagePath) {
        if (imagePath.startsWith("file:///android_asset/")) {
            try (InputStream inputStream = mContext.getAssets().open(imagePath.replaceFirst("file:///android_asset/", ""))) {
                GlUtil.bindImageTexture(textureId, inputStream);
            } catch (IOException e) {
                GlUtil.bindImageTexture(textureId, imagePath);
            }
        }
    }


    @Override
    public boolean autoSetup() {
        return mImagePathList != null;
    }


    public static class LookupFilterScroller {
        private static final int THRESHOLD_VELOCITY = 150; //滑动速度判断的阀值
        private Handler mRenderEventHandler;
        protected VelocityTracker mVelocityTracker;

        private GlLutFilterPager mLookupFilterPager;

        private int mTouchSlop;
        protected int mMinFlingVelocity;
        private Scroller mScroller;
        private float mLastDownX;
        private boolean mIsBeingDragged;
        private int mWidthRange;


        public LookupFilterScroller(Context context) {
            this(context, null);
        }

        public LookupFilterScroller(Context context, Handler renderEventHandler) {
            mRenderEventHandler = renderEventHandler;
            ViewConfiguration vc = ViewConfiguration.get(context);
            mTouchSlop = vc.getScaledTouchSlop();
            mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        }


        public void setup(GlLutFilterPager lookupFilterPager, int widthRange) {
            mLookupFilterPager = lookupFilterPager;
            mWidthRange = widthRange;
        }


        public boolean onInterceptTouchEvent(MotionEvent e) {
            if (!isAvailable()) return false;
            if (!mLookupFilterPager.isEnablePager()) return false;

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(e);
            final float x = e.getX();
            int action = e.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastDownX = x;
                    break;
                case MotionEvent.ACTION_MOVE:
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float diff = x - mLastDownX;
                    if (!mIsBeingDragged && mLookupFilterPager.canScrollX(diff)) {
                        if (Math.abs(diff) > mTouchSlop
                                || Math.abs(mVelocityTracker.getXVelocity()) > mMinFlingVelocity) {
                            mIsBeingDragged = true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsBeingDragged = false;
                    mVelocityTracker.clear();
                    break;
            }
            return mIsBeingDragged;
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (!isAvailable()) return false;
            if (!mLookupFilterPager.isEnablePager()) return false;
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mLastDownX = event.getX();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float diff = (event.getX() - mLastDownX);
                    if ((Math.abs(diff) > mTouchSlop)) {
                        mLookupFilterPager.scrollTo(diff / mWidthRange);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    float scX = mLookupFilterPager.getScrollX();
                    float xVelocity = mVelocityTracker.getXVelocity();
                    if (xVelocity < 0) {
                        if (scX <= -0.5f || xVelocity < -THRESHOLD_VELOCITY) {
                            smallScrollTo(-1f);
                        } else {
                            smallScrollTo(0f);
                        }
                    } else if (xVelocity > 0) {
                        if (scX >= 0.5f || xVelocity > THRESHOLD_VELOCITY) {
                            smallScrollTo(1f);
                        } else {
                            smallScrollTo(0f);
                        }
                    } else {
                        if (scX <= -0.5f) {
                            smallScrollTo(-1f);
                        } else if (scX >= 0.5f) {
                            smallScrollTo(1f);
                        } else {
                            smallScrollTo(0f);
                        }
                    }

                    mVelocityTracker.clear();
                    mIsBeingDragged = false;
                    break;
            }
            return mIsBeingDragged;
        }


        public void smallScrollTo(float scroll) {
            ValueAnimator animator = ValueAnimator.ofFloat(mLookupFilterPager.getScrollX(), scroll);
            long duration = (long) (200 * Math.abs((Math.abs(scroll) - (Math.abs(mLookupFilterPager.getScrollX())))));
            animator.setDuration(duration);

            animator.addUpdateListener(animation -> {
                float p = (float) animation.getAnimatedValue();
                mRenderEventHandler.post(() -> {
                    mLookupFilterPager.scrollTo(p);
                });

            });
            animator.start();
        }

        private boolean isAvailable() {
            return mLookupFilterPager != null && mRenderEventHandler != null
                    && mLookupFilterPager.isActive() && mLookupFilterPager.isSetup();
        }

        public void setRenderEventHandler(Handler renderEventHandler) {
            mRenderEventHandler = renderEventHandler;
        }


    }

    public interface OnPageChangeListener {
        void onPageSelected(int item);
    }

}
