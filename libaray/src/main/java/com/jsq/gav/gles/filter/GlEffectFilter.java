package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.GlUtil;

import java.nio.Buffer;

import static android.opengl.GLES20.GL_TEXTURE_2D;
import static com.jsq.gav.gles.GlConstants.FLOAT_SIZE_BYTES;

/**
 * Created by jiang on 2019/5/9
 */

public abstract class GlEffectFilter extends GlFilter {

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "gl_Position = aPosition;\n" +
            "textureCoordinate = aTextureCoord.xy;\n" +
            "}";

    private Buffer mPosCoordsVertices;
    private Buffer mTexCoordsVertices;


    private int maPositionHandle;
    private int maTextureHandle;


    public GlEffectFilter(Context context) {
        super(context);
    }

    @Override
    public void setup() {
        super.setup();
        mPosCoordsVertices = GlUtil.createFloatBuffer(GlConstants.FULL_RECTANGLE_COORDS);
        mTexCoordsVertices = GlUtil.createFloatBuffer(GlConstants.UV_COORDS);
        createProgram(VERTEX_SHADER, getFragmentShader());
        maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
    }

    protected abstract String getFragmentShader();

    @Override
    public int onDraw(int textureId) {
        GlUtil.glClearColor();
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgramHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mPosCoordsVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                2 * FLOAT_SIZE_BYTES, mTexCoordsVertices);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        perDraw();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // disable
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        return textureId;
    }

    private float getInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

    protected abstract void perDraw();


}
