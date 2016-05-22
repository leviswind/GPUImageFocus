package com.wuta.gpuimage.convert;

import com.wuta.gpuimage.util.OpenGlUtils;

/**
 * Created by kejin
 * on 2016/5/7.
 */
public class GPUImageConvertor
{
    public final static String TAG = "Convertor";

    protected GPUImageConvertFilter mRawConvertor = null;

    protected GPUImageTextureConvertFilter mTextureConvertor = null;

    protected ConvertType mConvertType = ConvertType.SURFACE_TEXTURE;

    public enum ConvertType {
        SURFACE_TEXTURE,
        RAW_NV21_TO_RGBA,
        RAW_YV12_TO_RGBA
    }

    public GPUImageConvertor()
    {
        this(ConvertType.RAW_NV21_TO_RGBA);
    }

    public GPUImageConvertor(ConvertType type)
    {
        mConvertType = type == null ? ConvertType.RAW_NV21_TO_RGBA : type;

        switch (mConvertType) {
            case SURFACE_TEXTURE:
                mTextureConvertor = new GPUImageTextureConvertFilter();
                mRawConvertor = null;
                break;

            case RAW_NV21_TO_RGBA:
                mRawConvertor = new GPUImageNV21ConvertFilter();
                mTextureConvertor = null;
                break;

            case RAW_YV12_TO_RGBA:
                mRawConvertor = new GPUImageYV12ConvertFilter();
                mTextureConvertor = null;
                break;
        }
    }

    public ConvertType getConvertType()
    {
        return mConvertType;
    }

    public void initialize()
    {
        if (mTextureConvertor != null) {
            mTextureConvertor.initialize();
        }
        else if (mRawConvertor != null) {
            mRawConvertor.initialize();
        }
    }

    public void onOutputSizeChanged(int width, int height)
    {
        if (mTextureConvertor != null) {
            mTextureConvertor.onOutputSizeChanged(width, height);
        }
        else if (mRawConvertor != null) {
//            GLES20.glUseProgram(mRawConvertor.getRawProgram());
            mRawConvertor.onOutputSizeChanged(width, height);
        }
    }

    public int convert(byte [] data, int width, int height)
    {
        if (mRawConvertor == null) {
            return OpenGlUtils.NO_TEXTURE;
        }
        return mRawConvertor.convert(data, width, height);
    }

    public int convert(int textureId)
    {
        if (mTextureConvertor == null) {
            return OpenGlUtils.NO_TEXTURE;
        }

        return mTextureConvertor.convert(textureId);
    }

    public void destroy()
    {
        if (mTextureConvertor != null) {
            mTextureConvertor.destroy();
        }

        if (mRawConvertor != null) {
            mRawConvertor.destroy();
        }
    }
}
