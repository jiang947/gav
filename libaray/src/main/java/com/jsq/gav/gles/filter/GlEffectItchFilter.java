package com.jsq.gav.gles.filter;

import android.content.Context;

import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform2fv;

/**
 * Created by jiang on 2019/5/12
 */

public class GlEffectItchFilter extends GlEffectFilter {

    private int mScanLineJitterLocation;
    private int mColorDriftLocation;
    private int mGlobalTimeLocation;

    private long mStartTime;

    private int mFrames = 0;

    /**
     * 动画总共8帧
     */
    private int mMaxFrames = 8;

    private float[] mDriftSequence = new float[]{0f, 0.03f, 0.032f, 0.035f, 0.03f, 0.032f, 0.031f, 0.029f, 0.025f};

    private float[] mJitterSequence = new float[]{0f, 0.03f, 0.01f, 0.02f, 0.05f, 0.055f, 0.03f, 0.02f, 0.025f};

    private float[] mThreshHoldSequence = new float[]{1.0f, 0.965f, 0.9f, 0.9f, 0.9f, 0.6f, 0.8f, 0.5f, 0.5f};

    public GlEffectItchFilter(Context context) {
        super(context);
    }

    @Override
    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision highp float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputTexture;\n" +
                "//这是个二阶向量，x是横向偏移的值，y是阈值\n" +
                "uniform vec2 uScanLineJitter;\n" +
                "//颜色偏移的值\n" +
                "uniform float uColorDrift;\n" +
                "//随机函数\n" +
                "float nrand(in float x, in float y)\n" +
                "{\n" +
                "    return fract(sin(dot(vec2(x, y), vec2(12.9898, 78.233))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "float u = textureCoordinate.x;\n" +
                "float v = textureCoordinate.y;\n" +
                "float jitter = nrand(v,0.0) * 2.0 - 1.0;\n" +
                "float drift = uColorDrift;\n" +
                "float offsetParam = step(uScanLineJitter.y,abs(jitter));\n" +
                "jitter = jitter * offsetParam * uScanLineJitter.x;\n" +
                "vec4 color1 = texture2D(inputTexture,fract(vec2( u + jitter,v)));\n" +
                "vec4 color2 = texture2D(inputTexture,fract(vec2(u + jitter + v*drift ,v)));\n" +
                "gl_FragColor = vec4(color1.r,color2.g,color1.b,1.0);\n" +
                "}";
    }


    @Override
    public void setup() {
        super.setup();
        mScanLineJitterLocation = glGetUniformLocation(mProgramHandle, "uScanLineJitter");
        mColorDriftLocation = glGetUniformLocation(mProgramHandle, "uColorDrift");
        mGlobalTimeLocation = glGetUniformLocation(mProgramHandle, "uGlobalTime");
    }

    @Override
    protected void perDraw() {
        long time = System.currentTimeMillis();
        if (mStartTime == 0) {
            mStartTime = time;
        }
        glUniform1f(mGlobalTimeLocation, mFrames);
        mStartTime = time;

        float slDisplacement = mJitterSequence[mFrames];
        float slThreshold = mThreshHoldSequence[mFrames];
        float drift = mDriftSequence[mFrames];
        mFrames++;
        if (mFrames > mMaxFrames) {
            mFrames = 0;
        }
        glUniform2fv(mScanLineJitterLocation, 1, new float[]{slDisplacement, slThreshold}, 0);
        glUniform1f(mColorDriftLocation, drift);
    }

    private float getInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

}
