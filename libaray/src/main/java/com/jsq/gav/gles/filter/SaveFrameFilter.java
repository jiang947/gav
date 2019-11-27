package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.GlUtil;

import java.nio.Buffer;

import static com.jsq.gav.gles.GlConstants.FLOAT_SIZE_BYTES;

/**
 * Created by jiang on 2019-08-23
 */

public class SaveFrameFilter extends GlFilter {


    private static final String VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";


    protected String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    protected int muMVPMatrixHandle;
    protected int muSTMatrixHandle;
    protected int maPositionHandle;
    protected int maTextureHandle;

    protected Buffer mPosCoordsVertices;
    protected Buffer mTexCoordsVertices;


    protected float[] mProjectionMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16]; // 变换之后的矩阵
    protected float[] mScratchMatrix = new float[16];
    private boolean mMatrixReady = false;
    private float mAngle;
    private float mWidth, mHeight;
    private float mPosX, mPosY;

    @Override
    public boolean needFrameBuffer() {
        return false;
    }

    public SaveFrameFilter(Context context) {
        super(context);
        createCoordsBuffer();
    }

    protected void createCoordsBuffer(){
        mPosCoordsVertices = GlUtil.createFloatBuffer(GlConstants.RECTANGLE_COORDS);
        mTexCoordsVertices = GlUtil.createFloatBuffer(GlConstants.UV_COORDS_ROTATION_180);
    }


    @Override
    public void setup() {
        super.setup();
        createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uSTMatrix");
    }

    @Override
    public int onDraw(int textureId) {
        Matrix.multiplyMM(mScratchMatrix, 0, this.mProjectionMatrix, 0, getModelViewMatrix(), 0);

        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mPosCoordsVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mTexCoordsVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mScratchMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, GlUtil.IDENTITY_MATRIX, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        return textureId;
    }

    public void setProjectionMatrix(float[] projectionMatrix) {
        mProjectionMatrix = projectionMatrix;
    }

    private void recomputeMatrix() {
        float[] modelView = mModelViewMatrix;

        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, mPosX, mPosY, 0.0f);
        if (mAngle != 0.0f) {
            Matrix.rotateM(modelView, 0, mAngle, 0.0f, 0.0f, 1.0f);
        }
        Matrix.scaleM(modelView, 0, mWidth, mHeight, 1.0f);
        mMatrixReady = true;
    }

    public float[] getModelViewMatrix() {
        if (!mMatrixReady) {
            recomputeMatrix();
        }
        return mModelViewMatrix;
    }

    public void setSize(float width, float height) {
        mWidth = width;
        mHeight = height;
        mMatrixReady = false;
    }

    public void setRotation(float angle) {
        while (angle >= 360.0f) {
            angle -= 360.0f;
        }
        while (angle <= -360.0f) {
            angle += 360.0f;
        }
        mAngle = angle;
        mMatrixReady = false;
    }

    public void setPosition(float posX, float posY) {
        mPosX = posX;
        mPosY = posY;
        mMatrixReady = false;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }
}
