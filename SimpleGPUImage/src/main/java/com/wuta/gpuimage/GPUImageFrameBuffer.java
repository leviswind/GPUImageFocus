package com.wuta.gpuimage;

/**
 * Created by kejin
 * on 2016/5/7.
 */

import android.opengl.GLES20;
import android.util.Log;

/**
 * 表示一个 FBO
 */
public class GPUImageFrameBuffer
{
    public final static String TAG = "GPUImageFrameBuffer";

    private int mFrameBufferId = 0;
    private int mFrameBufferTextureId = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    public boolean create(int width, int height)
    {
        int [] frameBuffer = new int[1];
        int [] frameBufferTexture = new int[1];

        // generate frame buffer
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        // generate texture
        GLES20.glGenTextures(1, frameBufferTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // set texture as colour attachment
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0);

        // unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        mFrameBufferId = frameBuffer[0];
        mFrameBufferTextureId = frameBufferTexture[0];

        mWidth = width;
        mHeight = height;

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "create framebuffer failed");
            return false;
        }
        Log.e(TAG, "create framebuffer success!");
        return true;
    }

    public void beginDrawToFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
        GLES20.glClearColor(0, 0, 0, 0);
    }

    public void showFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawToFrameBuffer(Runnable runnable)
    {
        beginDrawToFrameBuffer();
        runnable.run();
        showFrameBuffer();
    }

    public int getFrameBufferId()
    {
        return mFrameBufferId;
    }

    public int getFrameBufferTextureId()
    {
        return mFrameBufferTextureId;
    }

    public void destroy()
    {
        GLES20.glDeleteTextures(1, new int[]{mFrameBufferTextureId}, 0);
        GLES20.glDeleteBuffers(1, new int[]{mFrameBufferId}, 0);
    }
}
