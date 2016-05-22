package com.wuta.gpuimage.exfilters;

import android.opengl.GLES20;

import com.wuta.gpuimage.GPUImageFilter;
import com.wuta.gpuimage.GPUImageFilterGroup;
import com.wuta.gpuimage.util.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by kejin
 * on 2016/5/9.
 */
@Deprecated
public class GPUImageSampleFilter extends GPUImageFilterGroup
{
    public final static float TRANGLES [] = new float[] {
            -0.5f, 0.5f,
            -0.5f, -0.5f,
            0.5f, -0.5f,

            0.5f, -0.5f,
            0.5f, 0.5f,
            -0.5f, 0.5f
    };

    private int mMyProgId = 0;
    private int mAttrVertex = 0;
    private int mAttrTexture = 0;

    private FloatBuffer mTranglesBuffer;
    private int mArrayBufferId = 0;

    @Override
    public void onInitialized() {
        super.onInitialized();

        mTranglesBuffer = ByteBuffer.allocateDirect(TRANGLES.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTranglesBuffer.put(TRANGLES);

        mMyProgId = OpenGlUtils.loadProgram(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);

        if (mArrayBufferId == 0) {
            int[] buffer = new int[1];
            GLES20.glGenBuffers(1, buffer, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);

            mTranglesBuffer.position(0);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, TRANGLES.length, mTranglesBuffer, GLES20.GL_STATIC_DRAW);

            mArrayBufferId = buffer[0];
        }

        GLES20.glEnableVertexAttribArray(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mArrayBufferId);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, mTranglesBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDisableVertexAttribArray(0);
    }
}
