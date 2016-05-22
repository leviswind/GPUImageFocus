package com.wuta.gpuimage.convert;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by kejin
 * on 2016/5/6.
 */
public class GPUImageYV12ConvertFilter extends GPUImageConvertFilter
{

    public final static String TAG = "YV12";


    public final static String YV12_CONVERSION_FRAGMENT_SHADER =  "precision mediump float;\n" +
            "uniform sampler2D tex_y;\n" +
            "uniform sampler2D tex_u;\n" +
            "uniform sampler2D tex_v;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "vec4 c = vec4((texture2D(tex_y, textureCoordinate).r - 16./255.) * 1.164);\n" +
            "vec4 U = vec4(texture2D(tex_u, textureCoordinate).r - 128./255.);\n" +
            "vec4 V = vec4(texture2D(tex_v, textureCoordinate).r - 128./255.);\n" +
            "c += V * vec4(1.596, -0.813, 0, 0);\n" +
            "c += U * vec4(0, -0.392, 2.017, 0);\n" +
            "c.a = 1.0;\n" +
            "gl_FragColor = c;\n" +
            "}\n";


    private int mUniformYTexture;
    private int mUniformUTexture;
    private int mUniformVTexture;

    private int mYId = NO_TEXTURE;
    private int mUId = NO_TEXTURE;
    private int mVId = NO_TEXTURE;

    @Override
    public void initialize()
    {
        super.initialize();

        mUniformYTexture = GLES20.glGetUniformLocation(getRawProgram(), "tex_y");
        mUniformUTexture = GLES20.glGetUniformLocation(getRawProgram(), "tex_u");
        mUniformVTexture = GLES20.glGetUniformLocation(getRawProgram(), "tex_v");
    }

    @Override
    protected void onConvert(byte[] data, int width, int height) {
        /**
         * Covert Shader
         */
        long before = System.currentTimeMillis();
        ByteBuffer yBuf = ByteBuffer.wrap(data, 0, width*height);
        ByteBuffer vBuf = ByteBuffer.wrap(Arrays.copyOfRange(data, width*height, width*height+width*height/4));
        ByteBuffer uBuf = ByteBuffer.wrap(Arrays.copyOfRange(data, width*height*5/4, width*height*5/4+width*height/4));

        mYId = loadTexture(yBuf, width, height, mYId);
        mUId = loadTexture(uBuf, width/2, height/2, mUId);
        mVId = loadTexture(vBuf, width/2, height/2, mVId);

        Log.e(TAG, "Time: " + (System.currentTimeMillis() - before));

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYId);
        GLES20.glUniform1i(mUniformYTexture, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,  mUId);
        GLES20.glUniform1i(mUniformUTexture, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mVId);
        GLES20.glUniform1i(mUniformVTexture, 2);
    }
}
