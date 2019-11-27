//package com.jsq.video.simple.ui;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.opengl.GLUtils;
//import android.os.Bundle;
//import androidx.annotation.Nullable;
//import android.util.Log;
//
//import com.jsq.video.simple.base.BaseActivity;
//import com.lightgift.giftrenderer.LightGiftRenderer;
//import com.lightgift.giftrenderer.glesWrapper.OffScreenFrameBuffer;
//import com.lightgift.giftrenderer.render.IEventListener;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
//
///**
// * Created by jiang on 2019/7/9
// */
//
//public class LightGiftRenderGlSurfaceViewActivity extends BaseActivity {
//
//
//    private GLSurfaceView mSurfaceView;
//
//    public static void start(Context context) {
//        Intent starter = new Intent(context, LightGiftRenderGlSurfaceViewActivity.class);
//        context.startActivity(starter);
//    }
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mSurfaceView = new GLSurfaceView(this);
//        setContentView(mSurfaceView);
//        mSurfaceView.setEGLContextClientVersion(3);
//        mSurfaceView.setRenderer(new TestGiftRender(this));
//        LightGiftRenderer.setAuthEventListener(
//                new IEventListener() {
//                    @Override
//                    public int onEvent(int i, int i1, String s) {
//                        Log.e("", "MSG(type/ret/info):" + i + "/" + i1 + "/" + s);
//                        return 0;
//                    }
//                });
//
//        int id = LightGiftRenderer.auth(
//                this,
//                "UNt+gaVMRxqqOFqMKM1B1u1FwLCDP3u7Rs6KJsoBE7T1C9RVriQ10a4MJA1ov0Tb",
//                48);
//    }
//
//    private static class TestGiftRender implements GLSurfaceView.Renderer {
//        private Context mContext;
//        private LightGiftRenderer mGift;
//        private ImageRender mImageRender;
//        private ImageRender mPreviewRender;
//        private OffScreenFrameBuffer mOffScreenFrameBuffer;
//        private OffScreenFrameBuffer mOffScreenFrameBuffer1;
//
//        private TestGiftRender(Context context) {
//            mContext = context;
//        }
//
//        @Override
//        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//            mGift = new LightGiftRenderer(mContext, 0);
//            mGift.setGiftPath("assets/bunny_new/meta.json");
//            mImageRender = new ImageRender();
//            mPreviewRender = new ImageRender();
//
//            try (InputStream stream = mContext.getAssets().open("shaonv_20171026.png")) {
//                mImageRender.setImage(stream);
//            } catch (IOException ignore) {
//
//            }
//            GLES20.glEnable(GL10.GL_DITHER);
//        }
//
//        @Override
//        public void onSurfaceChanged(GL10 gl, int width, int height) {
//            mOffScreenFrameBuffer = new OffScreenFrameBuffer(1080, 1848);
//            mOffScreenFrameBuffer1 = new OffScreenFrameBuffer(1080, 1848);
//            GLES20.glViewport(0, 0, 1080, 1848);
//        }
//
//        @Override
//        public void onDrawFrame(GL10 gl) {
//            float[] m = {0.98303705f, 0.17768377f, 0.04546126f, 0.0f,
//                    -0.18143393f, 0.9783759f, 0.09930984f, 0.0f,
//                    -0.026832454f, -0.105873466f, 0.99401754f, 0.0f,
//                    0.07132529f, 0.07482066f, -1.500909f, 1.0f};
//            mOffScreenFrameBuffer.bind();
//            mImageRender.draw(mImageRender.getTextureId());
//            mOffScreenFrameBuffer.unbind();
//
//            mOffScreenFrameBuffer1.bind();
//            mGift.setModelView(m);
//            mGift.renderGift(mOffScreenFrameBuffer.getTexture(), 1080, 1848);
//            mOffScreenFrameBuffer1.unbind();
//
//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
//            mPreviewRender.draw(mOffScreenFrameBuffer1.getTexture());
//
//
//        }
//    }
//
//
//    private static class ImageRender {
//        private static final int SIZEOF_FLOAT = 4;
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
//        private static final float[] POS_COORDS = {
//                -1.0f, -1.0f,   // 0 bottom left
//                1.0f, -1.0f,   // 1 bottom right
//                -1.0f, 1.0f,   // 2 top left
//                1.0f, 1.0f,   // 3 top right
//        };
//        private static final float[] TXT_COORDS = {
//                0.0f, 0.0f,     // 0 bottom left
//                1.0f, 0.0f,     // 1 bottom right
//                0.0f, 1.0f,     // 2 top left
//                1.0f, 1.0f      // 3 top right
//        };
//
//        private ImageRender() {
//            mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
//            maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
//            maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
//            mPosCoordsVertices = createFloatBuffer(POS_COORDS);
//            mTexCoordsVertices = createFloatBuffer(TXT_COORDS);
//        }
//
//        public void setImage(InputStream inputStream) {
//            int[] textures = new int[1];
//            GLES20.glGenTextures(1, textures, 0);
//            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//            bitmap.recycle();
//            mTextureId = textures[0];
//        }
//
//        private int getTextureId() {
//            return mTextureId;
//        }
//
//
//        public void draw(int textureId) {
//
//
//            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
//            GLES20.glUseProgram(mProgramHandle);
//
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
//
//            GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
//                    2 * SIZEOF_FLOAT, mPosCoordsVertices);
//            GLES20.glEnableVertexAttribArray(maPositionHandle);
//
//            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
//                    2 * SIZEOF_FLOAT, mTexCoordsVertices);
//            GLES20.glEnableVertexAttribArray(maTextureHandle);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//
//            GLES20.glDisableVertexAttribArray(maPositionHandle);
//            GLES20.glDisableVertexAttribArray(maTextureHandle);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//
//            GLES20.glUseProgram(0);
//
//        }
//
//
//        public static FloatBuffer createFloatBuffer(float[] coords) {
//            FloatBuffer buffer = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            buffer.put(coords).position(0);
//            return buffer;
//        }
//
//        public static int createProgram(String vertexSource, String fragmentSource) {
//            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
//            if (vertexShader == 0) {
//                return 0;
//            }
//            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
//            if (pixelShader == 0) {
//                return 0;
//            }
//
//            int program = GLES20.glCreateProgram();
//            GLES20.glAttachShader(program, vertexShader);
//            GLES20.glAttachShader(program, pixelShader);
//            GLES20.glLinkProgram(program);
//            int[] linkStatus = new int[1];
//            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
//            if (linkStatus[0] != GLES20.GL_TRUE) {
//                GLES20.glDeleteProgram(program);
//                program = 0;
//            }
//            return program;
//        }
//
//
//        public static int loadShader(int shaderType, String source) {
//            int shader = GLES20.glCreateShader(shaderType);
//            GLES20.glShaderSource(shader, source);
//            GLES20.glCompileShader(shader);
//            int[] compiled = new int[1];
//            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
//            if (compiled[0] == 0) {
//                GLES20.glDeleteShader(shader);
//                shader = 0;
//            }
//            return shader;
//        }
//
//    }
//
//}
