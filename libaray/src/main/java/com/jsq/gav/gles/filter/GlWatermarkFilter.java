package com.jsq.gav.gles.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.jsq.gav.gles.GlUtil;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;

/**
 * Created by jiang on 2019/5/12
 */

public class GlWatermarkFilter extends GlSpriteFilter {

    private int mBitmapTextureId;

    private Bitmap mBitmap;

    public GlWatermarkFilter(Context context) {
        super(context);
    }

    @Override
    public void setup() {
        super.setup();
        mBitmapTextureId = GlUtil.genTextureId();
        bindingBitmapToTexture();
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        setSize(bitmap.getWidth(),bitmap.getHeight());
    }

    private void bindingBitmapToTexture() {
        if (mBitmap == null) return;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBitmapTextureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        mBitmap.recycle();
    }


    @Override
    public int onDraw(int textureId) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        super.onDraw(mBitmapTextureId);
        glDisable(GL_BLEND);
        return textureId;
    }
}
