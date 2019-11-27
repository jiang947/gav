package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;

import androidx.annotation.CallSuper;

import com.jsq.gav.LogUtil;
import com.jsq.gav.gles.GlUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiang on 2019/4/24
 */
// TODO: 2019-08-22 下次改版 filter 使用对象模式生成 OutputSurface,MediaComposer 等只封流程

public abstract class GlFilter {

    protected int mProgramHandle;
    protected int mVertexShader;
    protected int mPixelShader;

    protected final Context mContext;
    //    private final String mVertexShaderSource;
//    private final String mFragmentShaderSource;
    protected boolean mIsSetup = false;

    public GlFilter(Context context) {
        mContext = context;
    }

    protected Context getContext() {
        return mContext;
    }

    public void setupIfNeed() {
        if (!mIsSetup)
            setup();
    }

    @CallSuper
    public void setup() {
        mIsSetup = true;
    }

    /**
     * 这个filter是否自动setup
     *
     * @return 这个filter是否自动setup
     */
    public boolean autoSetup() {
        return true;
    }

    /**
     * 这个filter 是否启用
     *
     * @return 这个filter 是否启用
     */
    public boolean isActive() {
        return true;
    }

    /**
     * 是否需要FrameBufferObject
     * filter 在初始化的时候会来查, 如果需要就会自动配置一个FrameBufferObject
     *
     * @return true：需要
     */
    public boolean needFrameBuffer() {
        return true;
    }

    @CallSuper
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
        GLES20.glDeleteShader(mVertexShader);
        GLES20.glDeleteShader(mPixelShader);
        mIsSetup = false;
    }

    public abstract int onDraw(int textureId);

    public boolean isSetup() {
        return mIsSetup;
    }

    /**
     * 创建opengl 程序
     */
    protected int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GlUtil.checkGlError("glCreateProgram");
        if (program == 0) {
            LogUtil.e("GlFilter: Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        GlUtil.checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        GlUtil.checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogUtil.e("GlFilter: Could not link program: ");
            LogUtil.e("GlFilter: GLES20.glGetProgramInfoLog(program)");
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        mVertexShader = vertexShader;
        mPixelShader = pixelShader;
        mProgramHandle = program;
        return program;
    }

    protected String loadShaderFormAssets(String filepath) {
        byte[] buffer = new byte[4096];
        try (InputStream inputStream = mContext.getAssets().open(filepath);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            int l = 0;
            while ((l = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, l);
            }
            byteArrayOutputStream.flush();
            return new String(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            LogUtil.w("load shader in path :\"" + filepath + "\" failed");
            return "";
        }
    }

    public void setProjectionMatrix(float[] projectionMatrix) {
    }


}
