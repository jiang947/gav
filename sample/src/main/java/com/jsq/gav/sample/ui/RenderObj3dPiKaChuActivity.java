package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.jsq.gav.LogUtil;
import com.jsq.gav.gles.GlUtil;
import com.jsq.gav.gles.ObjModel;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by jiang on 2019/6/28
 */

public class RenderObj3dPiKaChuActivity extends BaseActivity {


    public static void start(Context context) {
        Intent starter = new Intent(context, RenderObj3dPiKaChuActivity.class);
        context.startActivity(starter);
    }

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render_obj3d);
        mGLSurfaceView = findViewById(R.id.surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);

        try {
            ObjModel.read(this,"pikachu/hat.obj");
//            mGLSurfaceView.setRenderer(new ObjRender(this, )));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class ObjRender implements GLSurfaceView.Renderer {

        private final Context mContext;
        private int mProgramHandle;

        protected int mHPosition;
        protected int mHCoord;
        protected int mHMatrix;
        protected int mHTexture;
        private ObjModel mObjModel;

        private int mHNormal;
        float[] matrix = new float[16];

        ObjRender(Context context, ObjModel objModel) {
            mContext = context;
            mObjModel = objModel;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mProgramHandle = GlUtil.createProgram(loadShaderFormAssets("pikachu/obj.vert"), loadShaderFormAssets("pikachu/obj.frag"));
            mHPosition = GLES20.glGetAttribLocation(mProgramHandle, "vPosition");
            mHCoord = GLES20.glGetAttribLocation(mProgramHandle, "vCoord");
            mHMatrix = GLES20.glGetUniformLocation(mProgramHandle, "vMatrix");
            mHTexture = GLES20.glGetUniformLocation(mProgramHandle, "vTexture");
            mHNormal = GLES20.glGetAttribLocation(mProgramHandle, "vNormal");

            matrix = new float[16];
            Matrix.setIdentityM(matrix, 0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.scaleM(matrix, 0, 0.2f, 0.2f * width / height, 0.2f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
//            Matrix.rotateM(matrix, 0, 0.3f, 0, 1, 0);
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glUseProgram(mProgramHandle);

            GLES20.glUniformMatrix4fv(mHMatrix, 1, false, matrix, 0);

            GLES20.glEnableVertexAttribArray(mHPosition);
            GLES20.glVertexAttribPointer(mHPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, mObjModel.getPosVertices());
            GLES20.glEnableVertexAttribArray(mHNormal);
            GLES20.glVertexAttribPointer(mHNormal, 3, GLES20.GL_FLOAT, false, 3 * 4, mObjModel.getNorVertices());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObjModel.getPosVertexCount());
            GLES20.glDisableVertexAttribArray(mHPosition);
            GLES20.glDisableVertexAttribArray(mHNormal);

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
    }


    /**
     * test gift
     */
    class Gift implements GLSurfaceView.Renderer {

//        private static final String VERTEX_SHADER = "" +
//                "attribute vec3 aPosition;\n" +
//                "attribute vec3 normal;\n" +
//                "attribute vec2 a_texCoord;\n" +
//                "varying vec2 v_texCoord;\n" +
//                "uniform mat4 model;\n" +
//                "uniform mat4 view;  \n" +
//                "uniform mat4 projection;  \n" +
//                "void main() {\n" +
//                "gl_Position =  projection * view * model * vec4(aPosition, 1.0);\n" +
//                "gl_Position.y = -gl_Position.y;\n" +
//                "v_texCoord = a_texCoord;  \n" +
//                "}";

//        private static final String FRAGMENT_SHADER = "" +
//                "precision mediump float; \n" +
//                "varying vec2 v_texCoord ;\n" +
//                "uniform sampler2D texture_diffuse0;\n" +
//                "uniform float alpha;  \n" +
//                "void main() {\n" +
//                "gl_FragColor = texture2D( texture_diffuse0, v_texCoord); \n" +
//                "gl_FragColor.a *= alpha;     \n" +
//                "}";


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

        private static final String FRAGMENT_SHADER = "" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";


        private int maPositionHandle;


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, "");
            if (vertexShader == 0) {
                return;
            }
            int pixelShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, "");
            if (pixelShader == 0) {
                return;
            }

            int program = GLES20.glCreateProgram();
            GlUtil.checkGlError("glCreateProgram");
            if (program == 0) {
                LogUtil.e("GlFilter: Could not create program");
            }
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                LogUtil.e("GlFilter: Could not link program: ");
                LogUtil.e("GlFilter: GLES20.glGetProgramInfoLog(program)");
                GLES20.glDeleteProgram(program);
                program = 0;
            }
//                mVertexShader = vertexShader;
//                mPixelShader = pixelShader;
//                mProgramHandle = program;

            maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");


            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }


}
