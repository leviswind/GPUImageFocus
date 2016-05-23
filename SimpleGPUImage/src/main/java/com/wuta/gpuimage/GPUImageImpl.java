package com.wuta.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;


import com.wuta.gpuimage.convert.GPUImageConvertor;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter2;
import com.wuta.gpuimage.glrecorder.GLRecorder;
import com.wuta.gpuimage.util.FPSMeter;
import com.wuta.gpuimage.util.OpenGlUtils;
import com.wuta.gpuimage.util.TextureRotationUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

/**
 * Created by kejin
 * on 2016/5/11.
 */
public class GPUImageImpl implements IGPUImage
{
    public final static String TAG = "GPUImage";

    public final Object mSurfaceChangedWaiter = new Object();

    public final static float [] CUBE = OpenGlUtils.VERTEX_CUBE;

    public final static float [] VERTEX_TRIANGLES = OpenGlUtils.VERTEX_TRIANGLES;

    public final static float [] TEXTURE_TRIANGLES = OpenGlUtils.TEXTURE_TRIANGLES;
    public final static float [] TEXTURE_TRIANGLES2 = OpenGlUtils.TEXTURE_TRIANGLES2;

    protected Context mContext;
    protected GLSurfaceView mGLSurfaceView;
    protected GPUImageFilter mImageFilter;
    protected GPUImageDrawFilter mDrawFilter;
    protected GPUImageDrawFilter2 mDrawFilter2;
    protected ScaleType mScaleType = ScaleType.CENTER_CROP;

    protected Camera mCamera;

    private EGLConfig mEGLConfig;

    /**
     * has converted texture handle id
     */
    private int mConvertedTextureId = NO_IMAGE;

    /**
     * surfacetexture's texture handle id
     */
    private int mSurfaceTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;

    private final GPUImageConvertor mImageConvertor;

    /**
     * runnable queue
     */
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private Vector<GPUImageFilter> vec= new Vector<GPUImageFilter>();
    private GPUImageFrameBuffer mFrameBuffer;

    private int mOutputWidth;
    private int mOutputHeight;
    private int mImageWidth;
    private int mImageHeight;

    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;
    private FaceDetector detector;
    private Face[] faces;

    private int mCount;
    private ByteBuffer mPixelBuf;
    private Bitmap mBitmap;

    public GPUImageImpl(Context context, GLSurfaceView view)
    {
        this(context, view, GPUImageConvertor.ConvertType.RAW_NV21_TO_RGBA);
    }

    public GPUImageImpl(Context context, GLSurfaceView view, GPUImageConvertor.ConvertType convertType)
    {
        this(context, convertType);
        setGLSurfaceView(view);
    }

    public GPUImageImpl(Context context, GPUImageConvertor.ConvertType convertType)
    {
        if (!OpenGlUtils.supportOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        mImageFilter = new GPUImageFilter();
        mImageConvertor = new GPUImageConvertor(convertType);

        mRunOnDraw = new LinkedList<Runnable>();
        mRunOnDrawEnd = new LinkedList<Runnable>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
        vec.add(new GPUImageDrawFilter(VERTEX_TRIANGLES,TEXTURE_TRIANGLES));
        mPixelBuf = ByteBuffer.allocate(mOutputWidth*mOutputHeight*4);
        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
        faces = new Face[2];
       // vec.add(new GPUImageDrawFilter(VERTEX_TRIANGLES,TEXTURE_TRIANGLES2));
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEGLConfig = config;
        mCount = 0;
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mImageFilter.init();
        for(GPUImageFilter filter:vec)
        {
            filter.init();
        }
        mImageConvertor.initialize();
    }
    @Override
    public void setFocus(MotionEvent event)
    {
        setFocus(event, mCamera);
    }
    @Override
    public void setFocus(MotionEvent event, Camera mCamera)
    {
        int AREA_SIZE = 200;
        mCamera.cancelAutoFocus();
        Camera.Parameters p = mCamera.getParameters();
        Log.e("setFocus","getRawX "+event.getRawX()+"   getRawY "+event.getRawY());
        Log.e("mOutput","mOutputWidth "+mOutputWidth+"  mOutputHeight: "+mOutputHeight);
        float touchX = (event.getRawX() / mOutputWidth) * 2000 - 1000;
        float touchY = (event.getRawY() / mOutputHeight) * 2000 - 1000;
        int left = clamp((int) touchX - AREA_SIZE / 2, -1000, 1000);
        int right = clamp(left + AREA_SIZE, -1000, 1000);
        int top = clamp((int) touchY - AREA_SIZE / 2, -1000, 1000);
        int bottom = clamp(top + AREA_SIZE, -1000, 1000);
        Rect rect = new Rect(left, top, right, bottom);
        Log.e("axis parameters"," "+left+", "+right+", "+top+", "+bottom+" p.getMaxNumFocusAreas() "+p.getMaxNumFocusAreas());
        if (p.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> areaList = new ArrayList<Camera.Area>();
            areaList.add(new Camera.Area(rect, 1000));
            p.setFocusAreas(areaList);
        }
        List<Camera.Area> area = new ArrayList<Camera.Area>();
        area = p.getFocusAreas();
        Rect rect2 = area.get(0).rect;
        Log.e("area"," "+rect2.left+" "+rect2.right+" "+rect2.top+" "+rect2.bottom);
        if (p.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areaList = new ArrayList<Camera.Area>();
            areaList.add(new Camera.Area(rect, 1000));
            p.setMeteringAreas(areaList);
        }
        final String currentFocusMode = p.getFocusMode();
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        try {
            mCamera.setParameters(p);
        } catch (Exception e) {

        }
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }
    private int clamp(int x, int min, int max) {//保证坐标必须在min到max之内，否则异常
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        detector = new FaceDetector(mOutputWidth,mImageHeight,2);

        GLRecorder.init(width, height, mEGLConfig/*Assign in onSurfaceCreated method*/);
        GLRecorder.setRecordOutputFile("/sdcard/glrecord.mp4");     // Set output file path

        mImageConvertor.onOutputSizeChanged(width, height);

        GLES20.glUseProgram(mImageFilter.getProgram());
        mImageFilter.onOutputSizeChanged(width, height);

        for(GPUImageFilter filter:vec)
        {
            GLES20.glUseProgram((filter.getProgram()));
            filter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
        }

        adjustImageScaling();

        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        FPSMeter.meter("DrawFrame");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(mRunOnDraw);
        switch (mImageConvertor.getConvertType()) {
            case SURFACE_TEXTURE:
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.updateTexImage();
                    mConvertedTextureId = mImageConvertor.convert(mSurfaceTextureId);
                }
                break;
        }
        int tempTextureId;
        mCount += 1;

        if (mCount == 200) {
//            GLRecorder.startRecording();
        } else if (mCount == 2000) {
//            GLRecorder.stopRecording();
        }

//        GLRecorder.beginDraw();

        //mDrawFilter.onDrawPicture(mGLVertexTrianglesBuffer, mGLTextureTrianglesBuffer, 2);

        //tempTextureId = mImageFilter.onDrawFrameBuffer(mConvertedTextureId, mGLCubeBuffer, mGLTextureBuffer);
        //tempTextureId = mImageFilter2.onDrawFrameBuffer(tempTextureId, mGLCubeBuffer, mGLTextureBuffer);
        //tempTextureId = mImageFilter3.onDrawFrameBuffer(tempTextureId, mGLCubeBuffer, mGLTextureBuffer);

        int size = vec.size();
        int tempTexture;
        vec.get(0).setTexture(mConvertedTextureId);
        tempTextureId = vec.get(0).onDrawPicture();
        for(int i=1;i<size;i++)
        {
            vec.get(i).setTexture(tempTextureId);
            tempTextureId = vec.get(i).onDrawPicture(mPixelBuf);
        }

        mImageFilter.onDraw(tempTextureId, mGLCubeBuffer, mGLTextureBuffer);
        mBitmap = Bitmap.createBitmap(mOutputWidth, mOutputHeight, Bitmap.Config.ARGB_8888);
        mPixelBuf.rewind();
        mBitmap.copyPixelsFromBuffer(mPixelBuf);
        /*int num = detector.findFaces(mBitmap,faces);
        PointF point = new PointF();
        faces[0].getMidPoint(point);
        Log.e("Detectored Faces","faces number: "+num+" MidPoint"+"("+point.x+","+point.y+")");
        mBitmap.recycle();
        mBitmap.copyPixelsFromBuffer(mPixelBuf);/*
        int num = detector.findFaces(mBitmap,faces);
        PointF point = new PointF();
        faces[0].getMidPoint(point);
        Log.e("Detectored Faces","faces number: "+num+" MidPoint"+"("+point.x+","+point.y+")");
    */
//        GLRecorder.endDraw();

        runAll(mRunOnDrawEnd);
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {

        FPSMeter.meter("PreviewFrame");
        if (data.length != mImageHeight*mImageWidth*3/2) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();
            mImageWidth = previewSize.width;
            mImageHeight = previewSize.height;
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    adjustImageScaling();
                }
            });
        }

        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    mConvertedTextureId = mImageConvertor.convert(data, mImageWidth, mImageHeight);
                    camera.addCallbackBuffer(data);
                }
            });
        }
        else {
            camera.addCallbackBuffer(data);
        }
    }

    @Override
    public void setPreviewSize(int width, int height) {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        mImageWidth = size.width;
        mImageHeight = size.height;

        adjustImageScaling();
    }

    @Override
    public void setupCamera(Camera camera) {
        setupCamera(camera, 0, false, false);
    }

    @Override
    public void setupCamera(final Camera camera, int degrees, boolean flipHor, boolean flipVer) {
        mCamera = camera;
        final Camera.Size size = camera.getParameters().getPreviewSize();
        mImageWidth = size.width;
        mImageHeight = size.height;

//        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        /**
         * 不使用 continuous 模式，自定义帧率
         */
        mGLSurfaceView.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestRender();
                mGLSurfaceView.postDelayed(this, 30);
            }
        },30);
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                setupSurfaceTexture(camera);

                camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                camera.setPreviewCallbackWithBuffer(GPUImageImpl.this);
                camera.startPreview();
            }
        });

        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        setRotation(rotation, flipHor, flipVer);
    }

    @Override
    public void setRotation(Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    @Override
    public void setRotation(Rotation rotation, boolean flipHor, boolean flipVer) {
        mFlipHorizontal = flipHor;
        mFlipVertical = flipVer;
        setRotation(rotation);
    }

    @Override
    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageFilter oldFilter = mImageFilter;
                mImageFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mImageFilter.init();
                GLES20.glUseProgram(mImageFilter.getProgram());
                mImageFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    @Override
    public void setDrawFilter(final GPUImageDrawFilter filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageDrawFilter oldFilter = mDrawFilter;
                mDrawFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mDrawFilter.init();
                GLES20.glUseProgram((mDrawFilter.getProgram()));
                mDrawFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    @Override
    public void setDrawFilter2(final GPUImageDrawFilter2 filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageDrawFilter2 oldFilter = mDrawFilter2;
                mDrawFilter2 = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mDrawFilter2.init();
                GLES20.glUseProgram((mDrawFilter2.getProgram()));
                mDrawFilter2.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    @Override
    public void setDrawPicture(final Bitmap picture) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mDrawFilter.setPicture(picture);
            }
        });
    }

    @Override
    public void setGLSurfaceView(GLSurfaceView surfaceView) {
        mGLSurfaceView = surfaceView;
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGLSurfaceView.setEGLConfigChooser(GLRecorder.getEGLConfigChooser());
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    @Override
    public void setBackgroundColor(float red, float green, float blue) {
        mBackgroundRed = red;
        mBackgroundGreen = green;
        mBackgroundBlue = blue;
    }

    @Override
    public void requestRender() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.requestRender();
        }
    }

    @Override
    public void destroy() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        mImageConvertor.destroy();
        mImageFilter.destroy();
    }

    private void setupSurfaceTexture(final Camera camera) {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }

        mSurfaceTextureId = createSurfaceTextureID();
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);

        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                FPSMeter.meter("FrameAvailableListenerFrame");
            }
        });
        try {
            camera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        if (mScaleType == ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    private int createSurfaceTextureID()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    public enum ScaleType { CENTER_INSIDE, CENTER_CROP }
}
