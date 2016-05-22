package com.wuta.gpuimage.convert;

import android.opengl.GLES20;

import com.wuta.gpuimage.GPUImageFrameBuffer;
import com.wuta.gpuimage.Rotation;
import com.wuta.gpuimage.util.OpenGlUtils;
import com.wuta.gpuimage.util.TextureRotationUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by kejin
 * on 2016/5/6.
 */
public abstract class GPUImageConvertFilter
{
    public final static String TAG = "RawFilter";

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };


    public final static int NO_TEXTURE = OpenGlUtils.NO_TEXTURE;

    private final String mVertexShader;
    private final String mFragmentShader;

    private int mRawProgId = 0;
    private int mAttribPosition;
    private int mUniformTexture;
    private int mAttribTextureCoordinate;

    protected boolean mIsInitialized = false;

    protected int mOutputWidth;
    protected int mOutputHeight;

    private final FloatBuffer mCubeBuffer;
    private final FloatBuffer mTextureBuffer;

    private GPUImageFrameBuffer mFrameBuffer = new GPUImageFrameBuffer();

    public GPUImageConvertFilter()
    {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public GPUImageConvertFilter(String vertexShader, String fragmentShader)
    {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

        mCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mCubeBuffer.put(CUBE).position(0);
    }

    public void initialize()
    {
        mRawProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
        mAttribPosition = GLES20.glGetAttribLocation(mRawProgId, "position");
        mUniformTexture = GLES20.glGetUniformLocation(mRawProgId, "inputImageTexture");
        mAttribTextureCoordinate = GLES20.glGetAttribLocation(mRawProgId, "inputTextureCoordinate");

        mIsInitialized = true;
    }


    /**
     * 交给子类做此操作
     */
    protected abstract void onConvert(byte [] data,
                                      int width,
                                      int height);

    /**
     * 返回绑定到 framebuffer 的纹理id
     */
    public int convert(byte [] data, int width, int height)
    {
        if (!mIsInitialized || data == null || data.length == 0) {
            return NO_TEXTURE;
        }
        /**
         * 要把所有的输出都要包住
         */
        mFrameBuffer.beginDrawToFrameBuffer();

        GLES20.glUseProgram(mRawProgId);

        /**
         * Vertex Shader
         */
        mCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mCubeBuffer);
        GLES20.glEnableVertexAttribArray(mAttribPosition);

        mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(mAttribTextureCoordinate);

        onConvert(data, width, height);

        /**
         * Draw end
         */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mAttribPosition);
        GLES20.glDisableVertexAttribArray(mAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        mFrameBuffer.showFrameBuffer();

        return mFrameBuffer.getFrameBufferTextureId();
    }

    public int getFrameBufferTextureId()
    {
        return mFrameBuffer.getFrameBufferTextureId();
    }

    public int getRawProgram()
    {
        return mRawProgId;
    }

    public void useRawProgram() {
        GLES20.glUseProgram(mRawProgId);
    }

    protected int loadTexture(Buffer buffer, int width, int height, int usedTexId)
    {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE ) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,
                    width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer);
            textures[0] = usedTexId;
        }

        return textures[0];
    }

    public void onOutputSizeChanged(int width, int height)
    {
        mOutputWidth = width;
        mOutputHeight = height;

        mFrameBuffer.create(width, height);
    }

    public void destroy()
    {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mRawProgId);
        mFrameBuffer.destroy();
        onDestroy();
    }

    protected void onDestroy() {}
}
