package com.wuta.gpuimage.exfilters;

import android.opengl.GLES20;

import com.wuta.gpuimage.GPUImageFilter;
import com.wuta.gpuimage.util.OpenGlUtils;

import java.nio.FloatBuffer;

/**
 * Created by kejin
 * on 2016/5/10.
 */
@Deprecated
public class GPUImageTriangleFilter extends GPUImageFilter
{
//    private int mMyProgId = 0;
//    private int mAttrVertex = 0;
//    private int mAttrInputTexture = 0;
//
//    @Override
//    public void onInitialized() {
//        super.onInitialized();
//
//        mMyProgId = OpenGlUtils.loadProgram(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
//        mAttrVertex = GLES20.glGetAttribLocation(mMyProgId, "position");
//        mAttrInputTexture = GLES20.glGetAttribLocation(mMyProgId, "inputImageTexture");
//    }


    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        onDrawArraysPre();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
