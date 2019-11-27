package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;

/**
 * Created by jiang on 2019/5/12
 */

public class GlEffectShakeFilter extends GlEffectFilter {

    float mScale = 0f;
    float mOffset = 0f;
    private int mScaleHandle;

    public GlEffectShakeFilter(Context context) {
        super(context);
    }

    @Override
    protected String getFragmentShader() {
        return "precision highp float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputTexture;\n" +
                "uniform float speed;\n" +
                "void main() {\n" +
                "vec2 uv = textureCoordinate.xy;\n" +
                "vec2 scaleCoordinate = vec2((speed - 1.0) * 0.5 + uv.x / speed , (speed - 1.0) * 0.5 + uv.y / speed);\n" +
                "vec4 smoothColor = texture2D(inputTexture, scaleCoordinate);\n" +
                "// 计算红色通道偏移值\n" +
                "vec4 shiftRedColor = texture2D(inputTexture, scaleCoordinate + vec2(-0.1 * (speed - 1.0), - 0.1 *(speed - 1.0)));\n" +
                "// 计算绿色通道偏移值\n" +
                "vec4 shiftGreenColor = texture2D(inputTexture, scaleCoordinate + vec2(-0.075 * (speed - 1.0), - 0.075 *(speed - 1.0)));\n" +
                "// 计算蓝色偏移值\n" +
                "vec4 shiftBlueColor = texture2D(inputTexture, scaleCoordinate + vec2(-0.05 * (speed - 1.0), - 0.05 *(speed - 1.0)));\n" +
                "vec3 resultColor = vec3(shiftRedColor.r, shiftGreenColor.g, shiftBlueColor.b);\n" +
                "gl_FragColor = vec4(resultColor, smoothColor.a);\n" +
                "}\n";
    }


    @Override
    public void setup() {
        super.setup();
        mScaleHandle = GLES30.glGetUniformLocation(mProgramHandle, "speed");
    }

    @Override
    protected void perDraw() {
        mScale = 1.0f + 0.3f * getInterpolation(mOffset);
        mOffset += 0.06f;
        if (mOffset > 1.0f) {
            mOffset = 0.0f;
        }
        GLES20.glUniform1f(mScaleHandle, mScale);
    }

    private float getInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

}
