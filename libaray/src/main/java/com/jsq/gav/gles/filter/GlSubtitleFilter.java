package com.jsq.gav.gles.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.jsq.gav.gles.GlUtil;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;

/**
 * Created by jiang on 2019/5/10
 */

public class GlSubtitleFilter extends GlSpriteFilter {
    private static final int DEFAULT_BG_COLOR = 0x00000000;
    private static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_TEXT_SIZE = 100;

    private int mSubtitleTextureId;
    private String mText;
    private int mTextSize = DEFAULT_TEXT_SIZE;
    private Drawable mBackgroundDrawable;

    @ColorInt
    private int mTextColor = DEFAULT_TEXT_COLOR;

    public GlSubtitleFilter(Context context, String text) {
        this(context, text, DEFAULT_TEXT_SIZE);
    }


    public GlSubtitleFilter(Context context, String text, int textSize) {
        super(context);
        mText = text;
        mTextSize = textSize;
    }

    @Override
    public void setup() {
        super.setup();
        mSubtitleTextureId = GlUtil.genTextureId();
        bindingTextToTexture(mSubtitleTextureId);
    }

    public void setBackgroundColor(@ColorInt int color) {
        // TODO: 判断color 是否与last color 相等
//        if (mBackgroundDrawable != null) {
//            LogUtil.w("GlSubtitleFilter: mBackgroundDrawable != null");
//        }
        Drawable drawable = new ColorDrawable(color);
        setBackgroundDrawable(drawable);
    }


    public void setBackgroundDrawable(@DrawableRes int drawable) {
        Drawable newDrawable = mContext.getDrawable(drawable);
        setBackgroundDrawable(newDrawable);
    }

    public void setBackgroundDrawable(Drawable drawable) {
        mBackgroundDrawable = drawable;
        if (isSetup()) {
            bindingTextToTexture(mSubtitleTextureId);
        }
    }

    /***
     * 文字 convert bitmap 绑定到opengl 纹理
     * @param textureId
     */
    private void bindingTextToTexture(int textureId) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(mTextSize);
        paint.setColor(mTextColor);
        int textWidth = (int) StaticLayout.getDesiredWidth(mText, paint);
        int textHeight = mTextSize;
        setSize(textWidth, textHeight);
        Bitmap bitmap = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = new ColorDrawable(DEFAULT_BG_COLOR);
        }
        mBackgroundDrawable.draw(canvas);

        canvas.drawText(mText, (getWidth() - StaticLayout.getDesiredWidth(mText, paint)) / 2,
                getHeight() / 2 + getBaseline(paint), paint);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public void setText(String text) {
        mText = text;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    @Override
    public int onDraw(int textureId) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        super.onDraw(mSubtitleTextureId);
        glDisable(GL_BLEND);
        return textureId;
    }

    public static float getBaseline(Paint p) {
        Paint.FontMetrics fontMetrics = p.getFontMetrics();
        return (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent;
    }


}
