package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;

/**
 * Created by jiang on 2019/5/12
 */

public class GlEffectFlashFilter extends GlEffectFilter {


    private int mExposeHandle;
    private int mFrames;

    private int mMaxFrames = 8;

    private int mHalfFrames = mMaxFrames / 2;

    public GlEffectFlashFilter(Context context) {
        super(context);
    }

    @Override
    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputTexture;\n" +
                "//修改这个值，可以控制曝光的程度\n" +
                "uniform float uAdditionalColor;\n" +
                "void main()\n" +
                "{\n" +
                "vec4 color = texture2D(inputTexture,textureCoordinate.xy);\n" +
                "gl_FragColor = vec4(color.r + uAdditionalColor,color.g + uAdditionalColor,color.b + uAdditionalColor,color.a);\n" +
                "}";
    }

    @Override
    public void setup() {
        super.setup();
        mExposeHandle = GLES30.glGetUniformLocation(mProgramHandle, "uAdditionalColor");
    }

    @Override
    protected void perDraw() {
        float progress;
        if (mFrames <= mHalfFrames) {
            progress = mFrames * 1.0f / mHalfFrames;
        } else {
            progress = 2.0f - mFrames * 1.0f / mHalfFrames;
        }
        mFrames++;
        if (mFrames > mMaxFrames) {
            mFrames = 0;
        }
        GLES20.glUniform1f(mExposeHandle, progress);
    }
}
