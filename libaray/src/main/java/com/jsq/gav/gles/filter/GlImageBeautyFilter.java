package com.jsq.gav.gles.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.jsq.gav.gles.GlUtil;

import java.nio.Buffer;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2f;

/**
 * Created by jiang on 2019/4/25
 */

public class GlImageBeautyFilter extends GlFilter {

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "gl_Position = aPosition;\n" +
            "vTextureCoord = aTextureCoord.xy;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "\n" +
            "varying mediump vec2 vTextureCoord;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform vec2 singleStepOffset;\n" +
            "uniform mediump float params;\n" +
            "\n" +
            "const highp vec3 W = vec3(0.299,0.587,0.114);\n" +
            "vec2 blurCoordinates[24];\n" +
            "\n" +
            "float hardLight(float color)\n" +
            "{\n" +
            "\tif(color <= 0.5)\n" +
            "\t\tcolor = color * color * 2.0;\n" +
            "\telse\n" +
            "\t\tcolor = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
            "\treturn color;\n" +
            "}\n" +
            "\n" +
            "void main(){\n" +
            "\n" +
            "    vec3 centralColor = texture2D(inputImageTexture, vTextureCoord).rgb;\n" +
            "    blurCoordinates[0] = vTextureCoord.xy + singleStepOffset * vec2(0.0, -10.0);\n" +
            "    blurCoordinates[1] = vTextureCoord.xy + singleStepOffset * vec2(0.0, 10.0);\n" +
            "    blurCoordinates[2] = vTextureCoord.xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
            "    blurCoordinates[3] = vTextureCoord.xy + singleStepOffset * vec2(10.0, 0.0);\n" +
            "    blurCoordinates[4] = vTextureCoord.xy + singleStepOffset * vec2(5.0, -8.0);\n" +
            "    blurCoordinates[5] = vTextureCoord.xy + singleStepOffset * vec2(5.0, 8.0);\n" +
            "    blurCoordinates[6] = vTextureCoord.xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
            "    blurCoordinates[7] = vTextureCoord.xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
            "    blurCoordinates[8] = vTextureCoord.xy + singleStepOffset * vec2(8.0, -5.0);\n" +
            "    blurCoordinates[9] = vTextureCoord.xy + singleStepOffset * vec2(8.0, 5.0);\n" +
            "    blurCoordinates[10] = vTextureCoord.xy + singleStepOffset * vec2(-8.0, 5.0);\n" +
            "    blurCoordinates[11] = vTextureCoord.xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
            "    blurCoordinates[12] = vTextureCoord.xy + singleStepOffset * vec2(0.0, -6.0);\n" +
            "    blurCoordinates[13] = vTextureCoord.xy + singleStepOffset * vec2(0.0, 6.0);\n" +
            "    blurCoordinates[14] = vTextureCoord.xy + singleStepOffset * vec2(6.0, 0.0);\n" +
            "    blurCoordinates[15] = vTextureCoord.xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
            "    blurCoordinates[16] = vTextureCoord.xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
            "    blurCoordinates[17] = vTextureCoord.xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
            "    blurCoordinates[18] = vTextureCoord.xy + singleStepOffset * vec2(4.0, -4.0);\n" +
            "    blurCoordinates[19] = vTextureCoord.xy + singleStepOffset * vec2(4.0, 4.0);\n" +
            "\n" +
            "    float sampleColor = centralColor.g * 20.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[0]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[1]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[2]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[3]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[4]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[5]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[6]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[7]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[8]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[9]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[10]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[11]).g;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[12]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[13]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[14]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[15]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[16]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[17]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[18]).g * 2.0;\n" +
            "    sampleColor += texture2D(inputImageTexture, blurCoordinates[19]).g * 2.0;\n" +
            "\n" +
            "    sampleColor = sampleColor / 48.0;\n" +
            "\n" +
            "    float highPass = centralColor.g - sampleColor + 0.5;\n" +
            "\n" +
            "    for(int i = 0; i < 5;i++)\n" +
            "    {\n" +
            "        highPass = hardLight(highPass);\n" +
            "    }\n" +
            "    float luminance = dot(centralColor, W);\n" +
            "\n" +
            "    float alpha = pow(luminance, params);\n" +
            "\n" +
            "    vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;\n" +
            "\n" +
            "    gl_FragColor = vec4(mix(smoothColor.rgb, max(smoothColor, centralColor), alpha), 1.0);\n" +
            "}";

    public float posRectangleCoords[] = {
            -1.0f, -1.0f, // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public float texRectangleCoords[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    private Buffer posCoordsVertices;
    private Buffer texCoordsVertices;

//    private int muMVPMatrixHandle;
//    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private int mInputImageTextureHandle;
    private int mSingleStepOffsetHandle;
    private int mParamsHandle;


    private float mIntensity = 1f;
    private int mWidth;
    private int mHeight;

    public GlImageBeautyFilter(Context context) {
        super(context);
    }


    public void setIntensity(float intensity) {
        mIntensity = intensity;
    }

    public void setSize(int width,int height){
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void setup() {
        super.setup();
        posCoordsVertices = GlUtil.createFloatBuffer(posRectangleCoords);
        texCoordsVertices = GlUtil.createFloatBuffer(texRectangleCoords);

        createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        mInputImageTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "inputImageTexture");
        mSingleStepOffsetHandle = GLES20.glGetUniformLocation(mProgramHandle, "singleStepOffset");
        mParamsHandle = GLES20.glGetUniformLocation(mProgramHandle, "params");


        // Program 使用 build 模式 ?
    }

    @Override
    public int onDraw(int textureId) {
        GlUtil.glClearColor();

        GLES20.glUseProgram(mProgramHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        glUniform1i(mInputImageTextureHandle, 1);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, posCoordsVertices);

        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordsVertices);

        glUniform2f(mSingleStepOffsetHandle, 1.0f / mWidth, 1.0f / mHeight);
        glUniform1f(mParamsHandle, mIntensity);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableVertexAttribArray(maPositionHandle);
        glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        return textureId;
    }

}
