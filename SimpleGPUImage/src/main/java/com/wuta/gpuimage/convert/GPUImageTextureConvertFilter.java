package com.wuta.gpuimage.convert;

/**
 * Created by kejin
 * on 2016/5/11.
 */

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.NonNull;

import com.wuta.gpuimage.GPUImageFrameBuffer;
import com.wuta.gpuimage.Rotation;
import com.wuta.gpuimage.util.OpenGlUtils;
import com.wuta.gpuimage.util.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 用来渲染 SurfaceTexture
 */
public class GPUImageTextureConvertFilter {

    protected final static String VERTEX_SHADER = "" +
            "attribute vec4 vPosition;\n" +
            "attribute vec2 inputTextureCoordinate;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main()\n" +
            "{\n" +
                "gl_Position = vPosition;\n" +
                "textureCoordinate = inputTextureCoordinate;\n" +
            "}";

    protected final String FRAGMENT_SHADER ="" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES s_texture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
            "}";


    protected final static float VERTEX_POSITION[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    protected final static float TEXTURE_COORDINATE[] = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);

    private int mProgId = 0;
    private int mAttrPosition;
    private int mAttrTextureCoordinate;

    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mCoordinateBuffer;

    private boolean mInitialized = false;

    private GPUImageFrameBuffer mFrameBuffer = new GPUImageFrameBuffer();

    public GPUImageTextureConvertFilter()
    {
        mVertexBuffer = nativeByteBuffer(VERTEX_POSITION.length*4).asFloatBuffer().put(VERTEX_POSITION);
        mVertexBuffer.position(0);

        mCoordinateBuffer = nativeByteBuffer(TEXTURE_COORDINATE.length*4).asFloatBuffer()
                .put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true));
        mCoordinateBuffer.position(0);
    }

    public void initialize()
    {
        mProgId = OpenGlUtils.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mAttrPosition = GLES20.glGetAttribLocation(mProgId, "vPosition");
        mAttrTextureCoordinate = GLES20.glGetAttribLocation(mProgId, "inputTextureCoordinate");

        mInitialized = true;
    }

    public void onOutputSizeChanged(int width, int height)
    {
        mFrameBuffer.create(width, height);
    }

    public int convert(int surfaceTextureId)
    {
        if (!mInitialized || surfaceTextureId == OpenGlUtils.NO_TEXTURE) {
            return OpenGlUtils.NO_TEXTURE;
        }

        mFrameBuffer.beginDrawToFrameBuffer();

        GLES20.glUseProgram(mProgId);
        GLES20.glEnableVertexAttribArray(mAttrPosition);
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 8, mVertexBuffer);

        GLES20.glEnableVertexAttribArray(mAttrTextureCoordinate);
        GLES20.glVertexAttribPointer(mAttrTextureCoordinate, 2, GLES20.GL_FLOAT, false, 8, mCoordinateBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, surfaceTextureId);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mAttrPosition);
        GLES20.glDisableVertexAttribArray(mAttrTextureCoordinate);

        mFrameBuffer.showFrameBuffer();

        return mFrameBuffer.getFrameBufferTextureId();
    }

    @NonNull
    private ByteBuffer nativeByteBuffer(int size)
    {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public void destroy()
    {
        GLES20.glDeleteProgram(mProgId);
        mFrameBuffer.destroy();
    }
}
