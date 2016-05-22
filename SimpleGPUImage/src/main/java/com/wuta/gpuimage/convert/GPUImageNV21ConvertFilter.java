package com.wuta.gpuimage.convert;

import android.opengl.GLES20;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by kejin
 * on 2016/5/6.
 */
public class GPUImageNV21ConvertFilter extends GPUImageConvertFilter
{
    public final static String TAG = "NV21";

    public static final String YUV2RGBA_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform sampler2D mGLUniformTextureY;\n" +
            "uniform sampler2D mGLUniformTextureUV;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "const mat3 yuv2rgb = mat3(\n" +
            "        1, 0, 1.2802,\n" +
            "        1, -0.214821, -0.380589,\n" +
            "        1, 2.127982, 0\n" +
            ");\n" +
            "\n" +
            "void main() {\n" +
            "   vec3 yuv = vec3(\n" +
            "1.1643 * (texture2D(mGLUniformTextureY, textureCoordinate).r - 0.0625),\n" +
            "texture2D(mGLUniformTextureUV, textureCoordinate).a - 0.5,\n" +
            "texture2D(mGLUniformTextureUV, textureCoordinate).r - 0.5\n" +
            "   );\n" +
            "   vec3 rgb = yuv * yuv2rgb;\n" +
            "   gl_FragColor = vec4(rgb, 1);\n" +
            "}\n";

    private int mGLUniformTextureY;
    private int mGLUniformTextureUV;
    private int mYuv2rgbMatrixLocation;

    private byte [] mUVData = null;

    private int mYTexture = NO_TEXTURE;
    private int mUVTexture = NO_TEXTURE;

    public GPUImageNV21ConvertFilter()
    {
        super(NO_FILTER_VERTEX_SHADER, YUV2RGBA_FRAGMENT_SHADER);
    }

    @Override
    protected void onDestroy() {
        GLES20.glDeleteTextures(2, new int[] {mYTexture, mUVTexture}, 0);
    }

    @Override
    public void initialize() {
        super.initialize();

        mGLUniformTextureY = GLES20.glGetUniformLocation(getRawProgram(), "mGLUniformTextureY");
        mGLUniformTextureUV = GLES20.glGetUniformLocation(getRawProgram(), "mGLUniformTextureUV");
    }


    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
    }

    @Override
    protected void onConvert(byte[] data, int width, int height) {
        if (mUVData == null) {
            mUVData = new byte[width*height/2];
        }
        System.arraycopy(data, width*height, mUVData, 0, width*height/2);

        ByteBuffer yBuf = ByteBuffer.wrap(data, 0, width*height);
        ByteBuffer uvBuf = ByteBuffer.wrap(mUVData);

        mYTexture = loadYTexture(yBuf, width, height, mYTexture);
        mUVTexture = loadUVTexture(uvBuf, width/2,height/2, mUVTexture);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYTexture);
        GLES20.glUniform1i(mGLUniformTextureY, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUVTexture);
        GLES20.glUniform1i(mGLUniformTextureUV, 1);
    }

    private int loadYTexture(Buffer buffer, int width, int height, int usedTextId)
    {
        if (usedTextId == NO_TEXTURE) {
            int [] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height,
                    0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer);

            usedTextId = textures[0];
        }
        else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTextId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer);

        }

        return usedTextId;
    }

    private int loadUVTexture(Buffer buffer, int width, int height, int usedTextId)
    {
        if (usedTextId == NO_TEXTURE) {
            int [] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, width, height,
                    0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, buffer);

            usedTextId = textures[0];
        }
        else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTextId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        return usedTextId;
    }
}
