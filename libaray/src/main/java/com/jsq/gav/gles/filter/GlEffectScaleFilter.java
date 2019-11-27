package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;

import java.nio.Buffer;

/**
 * Created by jiang on 2019/5/10
 */

public class GlEffectScaleFilter extends GlEffectFilter {


    private Buffer mPosCoordsVertices;
    private Buffer mTexCoordsVertices;


    private int maPositionHandle;
    private int maTextureHandle;
    private int mScaleHandle;

    private boolean plus = false;
    float mScale = 0f;
    float mOffset = 0f;

    public GlEffectScaleFilter(Context context) {
        super(context);

    }

    @Override
    public void setup() {
        super.setup();
        mScaleHandle = GLES30.glGetUniformLocation(mProgramHandle, "speed");
    }

    @Override
    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputTexture;\n" +
                "uniform float speed;\n" +
                "void main() {\n" +
                "vec2 uv = textureCoordinate.xy;\n" +
                "// 将纹理坐标中心转成(0.0, 0.0)再做缩放\n" +
                "vec2 center = vec2(0.5, 0.5);\n" +
                "uv -= center; uv = uv / speed;\n" +
                "uv += center;\n" +
                "gl_FragColor = texture2D(inputTexture, uv);\n" +
                "}\n";
    }


    private void scale() {
        mOffset += plus ? +0.06f : -0.06f;
        if (mOffset >= 1.0f) {
            plus = false;
        } else if (mOffset <= 0.0f) {
            plus = true;
        }
        mScale = 1.0f + 0.5f * getInterpolation(mOffset);
        GLES20.glUniform1f(mScaleHandle, mScale);
    }

    private float getInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

    @Override
    protected void perDraw() {
        scale();
    }

}
