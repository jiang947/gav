//package com.jsq.video.simple.ui;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.SurfaceTexture;
//import android.opengl.GLES20;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.Message;
//import androidx.annotation.Nullable;
//import android.view.TextureView;
//
//import com.jsq.video.gles.EglCore;
//import com.jsq.video.gles.FrameBufferObject;
//import com.jsq.video.gles.GlConstants;
//import com.jsq.video.gles.GlUtil;
//import com.jsq.video.gles.WindowSurface;
//import com.jsq.video.gles.filter.GlWatermarkFilter;
//import com.jsq.video.simple.R;
//import com.jsq.video.simple.base.BaseActivity;
//import com.jsq.video.simple.filter.MultiLightGiftFilter;
//import com.lightgift.giftrenderer.LightGiftRenderer;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.FloatBuffer;
//
//import static com.jsq.video.gles.GlConstants.FLOAT_SIZE_BYTES;
//
///**
// * Created by jiang on 2019/7/8
// */
//
//public class LightGiftRenderActivity extends BaseActivity implements TextureView.SurfaceTextureListener {
//
//    private static final int MSG_PREPARE = 1;
//    private TextureView mTextureView;
//
//    private HandlerThread mRenderThread;
//    private Handler mRenderEventHandler;
//
//    private EglCore mEglCore;
//    private WindowSurface mWindowSurface;
//
//    private MultiLightGiftFilter mMultiLightGiftFilter;
//    private LightGiftRenderer mGift;
//    private GlWatermarkFilter mGlWatermarkFilter;
//    private ImageRender mImageRender;
//    private ImageRender mPreviewRender;
//
//    private FrameBufferObject mFrameBufferObject;
//    private FrameBufferObject mFrameBufferObject1;
//
//
//    public static void start(Context context) {
//        Intent starter = new Intent(context, LightGiftRenderActivity.class);
//        context.startActivity(starter);
//    }
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_light_gift_render);
//        mTextureView = findViewById(R.id.texture_view);
//        mTextureView.setSurfaceTextureListener(this);
//
//    }
//
//    private boolean handlerMessage(Message msg) {
//        prepare((SurfaceTexture) msg.obj, msg.arg1, msg.arg2);
//        return true;
//    }
//
//
//    private void prepare(SurfaceTexture surfaceTexture, int width, int height) {
//        mEglCore = new EglCore(null,EglCore.FLAG_TRY_GLES3);
//        mWindowSurface = new WindowSurface(mEglCore, surfaceTexture);
//        mWindowSurface.makeCurrent();
//        mMultiLightGiftFilter = new MultiLightGiftFilter(this);
//        mMultiLightGiftFilter.setup();
//        mGlWatermarkFilter = new GlWatermarkFilter(this);
//        mGlWatermarkFilter.setup();
//        mImageRender = new ImageRender();
//        mPreviewRender = new ImageRender();
//        mGift = new LightGiftRenderer(this, 1);
//        mGift.setGiftPath("assets/bunny_new/meta.json");
//        try (InputStream stream = getAssets().open("lookup.png")) {
//            mImageRender.setImage(stream);
////            mGlWatermarkFilter.setBitmap(BitmapFactory.decodeStream(stream));
////            mGlWatermarkFilter.setSize(width, height);
////            mGlWatermarkFilter.setPosition(width >> 1, height >> 1);
//        } catch (IOException ignore) {
//
//        }
//        mFrameBufferObject = new FrameBufferObject();
//        mFrameBufferObject1 = new FrameBufferObject();
//        mFrameBufferObject.setup(1080, 1920);
//        mFrameBufferObject1.setup(1080, 1920);
//        GLES20.glViewport(0, 0, width, height);
////        surfaceTexture.setOnFrameAvailableListener(this::drawFrame);
//        mFrameBufferObject.bind();
//        mImageRender.draw(mImageRender.getTextureId());
////        float []m = {0.98303705f, 0.006781f, -0.028911f, 0.000000f, 0.1756946f, 0.97004527f, -0.16776077f, 0.000000f,
////                -0.11189723f, 0.18898544f, 0.9755837f, 0.000000f, 0.29386932f, 0.25524125f, -2.5241039f, 1.000000f};
////
//        float[] m = {0.98303705f, 0.17768377f, 0.04546126f, 0.0f,
//                -0.18143393f, 0.9783759f, 0.09930984f, 0.0f,
//                -0.026832454f, -0.105873466f, 0.99401754f, 0.0f,
//                0.07132529f, 0.07482066f, -1.500909f, 1.0f};
//        mFrameBufferObject1.bind();
//        mGift.setModelView(m);
//        mGift.renderGift(mFrameBufferObject.getTextureId(), 1080, 1920);
//
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
//        mPreviewRender.draw(mFrameBufferObject1.getTextureId());
//
//        mWindowSurface.swapBuffers();
//    }
//
//    @Override
//    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        mRenderThread = new HandlerThread("render-thread");
//        mRenderThread.start();
//        mRenderEventHandler = new Handler(mRenderThread.getLooper(), this::handlerMessage);
//        mRenderEventHandler.obtainMessage(MSG_PREPARE, width, height, surface).sendToTarget();
//
//    }
//
//    @Override
//    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//    }
//
//    @Override
//    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        return false;
//    }
//
//    @Override
//    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//    }
//
//
//    private static class ImageRender {
//
//        private int mTextureId;
//        private int maPositionHandle;
//        private int maTextureHandle;
//        private FloatBuffer mPosCoordsVertices;
//        private FloatBuffer mTexCoordsVertices;
//
//        private int mProgramHandle;
//
//        private static final String VERTEX_SHADER = "" +
//                "attribute vec4 aPosition;\n" +
//                "attribute vec4 aTextureCoord;\n" +
//                "varying highp vec2 vTextureCoord;\n" +
//                "void main() {\n" +
//                "gl_Position = aPosition;\n" +
//                "vTextureCoord = aTextureCoord.xy;\n" +
//                "}";
//
//        private static final String FRAGMENT_SHADER = "" +
//                "precision mediump float;\n" +
//                "varying vec2 vTextureCoord;\n" +
//                "uniform sampler2D sTexture;\n" +
//                "void main() {\n" +
//                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
//                "}\n";
//
//
//        public void setImage(InputStream inputStream) {
//            mTextureId = GlUtil.createImageTexture(inputStream);
//        }
//
//        private int getTextureId() {
//            return mTextureId;
//        }
//
//        public void draw(int textureId) {
//            mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
//            maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
//            maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
//            mPosCoordsVertices = GlUtil.createFloatBuffer(GlConstants.FULL_RECTANGLE_COORDS);
//            mTexCoordsVertices = GlUtil.createFloatBuffer(new float[]{0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f});
//
//            GlUtil.glClearColor();
//
//            GLES20.glUseProgram(mProgramHandle);
//
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
//
//            GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
//                    2 * FLOAT_SIZE_BYTES, mPosCoordsVertices);
//            GLES20.glEnableVertexAttribArray(maPositionHandle);
//
//            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
//                    2 * FLOAT_SIZE_BYTES, mTexCoordsVertices);
//            GLES20.glEnableVertexAttribArray(maTextureHandle);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        }
//
//    }
//
//
//}
