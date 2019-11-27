package com.jsq.gav.gles.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.GlUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.jsq.gav.gles.GlConstants.FLOAT_SIZE_BYTES;
import static com.jsq.gav.gles.GlUtil.TAG;

/**
 * Created by jiang on 2019/4/23
 *
 */
public class CameraFilter extends GlFilter {

    protected Buffer mPosCoordsVertices;
    protected Buffer mTexCoordsVertices;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";


    private float[] mProjectionMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16]; // 变换之后的矩阵
    private float[] mScratchMatrix = new float[16];
    private boolean mMatrixReady = false;
    private float mAngle;
    private float mWidth, mHeight;
    private float mPosX, mPosY;

    private int mProgram;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    public CameraFilter(Context context, float[] texCoords) {
        super(context);
        mPosCoordsVertices = GlUtil.createFloatBuffer(GlConstants.RECTANGLE_COORDS);
        mTexCoordsVertices = GlUtil.createFloatBuffer(texCoords);
    }

    @Override
    public int onDraw(int textureId) {
        Matrix.multiplyMM(mScratchMatrix, 0, this.mProjectionMatrix, 0, getModelViewMatrix(), 0);

        GlUtil.glClearColor();
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mPosCoordsVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mTexCoordsVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mScratchMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, GlUtil.IDENTITY_MATRIX, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // disable
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        return textureId;
    }

    @Override
    public void setup() {
        super.setup();
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        GlUtil.checkGlError("camera setup");
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

    /**
     * Saves the current frame to disk as a PNG image.  Frame starts from (0,0).
     * <p>
     * Useful for debugging.
     */
    // TODO: 2019/4/24 迁移到 outputSurface
    public static void saveFrame(String filename, int width, int height) {
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();
        int pixelCount = width * height;
        int[] colors = new int[pixelCount];
        buf.asIntBuffer().get(colors);
        for (int i = 0; i < pixelCount; i++) {
            int c = colors[i];
            colors[i] = (c & 0xff00ff00) | ((c & 0x00ff0000) >> 16) | ((c & 0x000000ff) << 16);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            Bitmap bmp = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            bmp.recycle();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to write file " + filename, ioe);
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException ioe2) {
                throw new RuntimeException("Failed to close file " + filename, ioe2);
            }
        }
        Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + filename + "'");
    }


}
