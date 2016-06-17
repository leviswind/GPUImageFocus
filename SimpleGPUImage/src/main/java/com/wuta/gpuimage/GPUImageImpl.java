package com.wuta.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ContextThemeWrapper;


import com.wuta.gpuimage.convert.GPUImageConvertor;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter2;
import com.wuta.gpuimage.glrecorder.GLRecorder;
import com.wuta.gpuimage.util.FPSMeter;
import com.wuta.gpuimage.util.OpenGlUtils;
import com.wuta.gpuimage.util.TextureRotationUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;
import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_ROTATED_90;
import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_ROTATED_180;
import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_ROTATED_270;
import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_SAVE;
import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_SAVE_CROP;


/**
 * Created by kejin
 * on 2016/5/11.
 */
public class GPUImageImpl implements IGPUImage
{
    public final static String TAG = "GPUImage";

    public final Object mSurfaceChangedWaiter = new Object();

    public final static float [] CUBE = OpenGlUtils.VERTEX_CUBE;
    public final static float [] CUBE_CROP = OpenGlUtils.VERTEX_CUBE_CROP;


    public final static float [] VERTEX_TRIANGLES = OpenGlUtils.VERTEX_TRIANGLES;

    public final static float [] TEXTURE_TRIANGLES = OpenGlUtils.TEXTURE_TRIANGLES;
    public final static float [] TEXTURE_TRIANGLES3 = OpenGlUtils.TEXTURE_TRIANGLES3;
    public final static float [] TEXTURE_TRIANGLES4 = OpenGlUtils.TEXTURE_TRIANGLES4;

    protected Context mContext;
    protected GLSurfaceView mGLSurfaceView;
    protected GPUImageFilter mImageFilter;
    protected GPUImageFilter mImageFilter2;
    protected GPUImageDrawFilter mDrawFilter;
    protected GPUImageDrawFilter2 mDrawFilter2;
    protected ScaleType mScaleType = ScaleType.CENTER_CROP;

    protected Camera mCamera;

    private EGLConfig mEGLConfig;

    /**
     * has converted texture handle id
     */
    private int mConvertedTextureId = NO_IMAGE;
    private int mConvertedTextureIdForSave = NO_IMAGE;

    /**
     * surfacetexture's texture handle id
     */
    private int mSurfaceTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;

    private final GPUImageConvertor mImageConvertor;
    private final GPUImageConvertor mImageConvertorForSave;

    /**
     * runnable queue
     */
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLCubeBuffer2;

    private final FloatBuffer mGLTextureBuffer;
    private FloatBuffer mPictureBuffer;
    private final FloatBuffer mSaveTextureBuffer;
    private final FloatBuffer mSaveTextureBuffer2;
    private Vector<GPUImageFilter> vec= new Vector<GPUImageFilter>();
    private boolean textureCropFlag = false;

    private int mOutputWidth;
    private int mOutputHeight;
    private int mImageWidth;
    private int mImageHeight;
    private int mPictureWidth;
    private int mPictureHeight;

    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;
    private Triangle mTriangle;

    private boolean save_flag = false;
    private boolean draw_flag = true;
    private  float triangleCoords[] = {   // in counterclockwise order:
            0.0f,  0.0f,
            -0.5f, 0.0f,
            -0.5f, -0.5f,
            0.0f,-0.5f,
            -1.0f,-1.0f,
            0.0f,0.0f
    };

    // Set color with red, green, blue and alpha (opacity) values
    private float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
    private static float temp = 0.3f;
    private static float center[] = {0.0f, 0.0f};
    private float VERTEX_CUBE[]={
                                    -temp+center[0], -temp+center[1],
                                    temp+center[0], -temp+center[1],
                                    -temp+center[0], temp+center[1],
                                    temp+center[0], temp+center[1]
    };
    public static boolean releaseFlag = false;
    private Bitmap bitmap;
    private Bitmap bitmapsave;

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
        mImageFilter2 = new GPUImageFilter();
        mImageConvertor = new GPUImageConvertor(convertType);
        mImageConvertorForSave = new GPUImageConvertor(GPUImageConvertor.ConvertType.RAW_NV21_TO_RGBA);

        mRunOnDraw = new LinkedList<Runnable>();
        mRunOnDrawEnd = new LinkedList<Runnable>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer2 = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);
        mPictureBuffer =ByteBuffer.allocateDirect(VERTEX_CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mPictureBuffer.put(VERTEX_CUBE).position(0);
        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSaveTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_ROTATED_180.length * 4)        //经过framebuffer会翻转，用于将读入贴图翻转，最后输出到屏幕又翻转回来了
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        mSaveTextureBuffer2 = ByteBuffer.allocateDirect(TEXTURE_ROTATED_180.length * 4)      //在ondraw不经过framebuffer时，翻转，用于最后输出到屏幕时翻转从framebuffer读出来的texture
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mSaveTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);
        setRotation(Rotation.NORMAL, false, false);
        vec.add(new GPUImageDrawFilter(VERTEX_TRIANGLES,TEXTURE_TRIANGLES));                //这种配置经过framebufffer，会保持不变，不翻转，用于嘻哈镜，4个三角形，所以参数比较多
        mDrawFilter = new GPUImageDrawFilter(VERTEX_TRIANGLES,TEXTURE_TRIANGLES);
    }

    @Override
    public void setBitmap(Bitmap bitmap1)
    {
        bitmap = bitmap1;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEGLConfig = config;
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mImageFilter.init();
        mImageFilter2.init();
        for(GPUImageFilter filter:vec)
        {
            filter.init();
        }
        mDrawFilter.init();
        mImageConvertor.initialize();
        mImageConvertorForSave.initialize();
        mCamera.startPreview();
        mCamera.startFaceDetection();
        mDrawFilter.setPicture(bitmap);
        mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(final Camera.Face[] faces, Camera camera) {
                if(faces.length > 0)
                {
                    runOnDraw(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("face detector","detected face-------------------------------     "+faces[0].rect.left+"  "+faces[0].rect.top+"   "+
                                    faces[0].rect.right+"   "+faces[0].rect.bottom);
                            triangleCoords = pixeltotexturecoor(faces[0].rect);
                            String s = "";
                            for(int i =0;i<10;i++)
                                s=s+triangleCoords[i]+",";
                            Log.e("triagleCoord","("+s+")");
                        }
                    });
                }
                else
                {
                    Log.e("no detected face","result--------------------");
                }
            }
        });
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
        //Log.e("setFocus","getRawX "+event.getRawX()+"   getRawY "+event.getRawY());
        //Log.e("mOutput","mOutputWidth "+mOutputWidth+"  mOutputHeight: "+mOutputHeight);

        float touchY = -(event.getRawX() / mOutputWidth) * 2000 + 1000;
        float touchX = -(event.getRawY() / mOutputHeight) * 2000 +1000;
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
                //params.setFocusMode(currentFocusMode);
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

        mImageConvertor.onOutputSizeChanged(width, height);
        mImageConvertorForSave.onOutputSizeChanged(mPictureHeight,mPictureWidth);

        GLES20.glUseProgram(mImageFilter.getProgram());
        mImageFilter.onOutputSizeChanged(width, height);
        GLES20.glUseProgram(mImageFilter2.getProgram());
        mImageFilter2.onOutputSizeChanged(mPictureHeight,mPictureWidth);

        for(GPUImageFilter filter:vec)
        {
            GLES20.glUseProgram((filter.getProgram()));
            filter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
        }
        GLES20.glUseProgram((mDrawFilter.getProgram()));
        mDrawFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);

        adjustImageScaling();

        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
    }
    private boolean textureFlag = true;
    @Override
    public void onDrawFrame(GL10 gl) {
        set_Crop();
        if(draw_flag)
        {
            FPSMeter.meter("DrawFrame");
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            runAll(mRunOnDraw);
            switch (mImageConvertor.getConvertType()) {
                case SURFACE_TEXTURE:
                    if (mSurfaceTexture != null || !releaseFlag ) {
                        mSurfaceTexture.updateTexImage();
                        mConvertedTextureId = mImageConvertor.convert(mSurfaceTextureId);
                    }
                    break;
            }
            int tempTextureId;
            mTriangle = new Triangle(triangleCoords,color);     //四边形

            int size = vec.size();
            int tempTexture;
            vec.get(0).setTexture(mConvertedTextureId);

            tempTextureId = vec.get(0).onDrawPicture();
            for(int i=1;i<size;i++)
            {
                vec.get(i).setTexture(tempTextureId);
                tempTextureId = vec.get(i).onDrawPicture();
            }
            center[0]+=0.01;
            if(center[0]-temp+0.01>1)                   //贴图向右移动，超出后重新从左边出现
                center[0] = -1;

            float VERTEX_CUBE[] = {
                    -temp+center[0], -temp+center[1],
                    temp+center[0], -temp+center[1],
                    -temp+center[0], temp+center[1],
                    temp+center[0], temp+center[1]
            };
            mPictureBuffer.rewind();
            mPictureBuffer.put(VERTEX_CUBE).position(0);
            //mImageFilter.onDraw(tempTextureId, mGLCubeBuffer, mGLTextureBuffer);

            int tempTexture2 = mDrawFilter.onDrawPicture();
            //mImageFilter.onDraw(tempTexture2, mPictureBuffer, mSaveTextureBuffer);
            mImageFilter.onDrawFrameBuffer(mConvertedTextureId, mGLCubeBuffer, mGLTextureBuffer);

            mImageFilter.onDrawFrameBuffer(tempTexture2, mPictureBuffer, mSaveTextureBuffer);
            mImageFilter.onDraw(mImageFilter.getFrameBufferTexture(), mGLCubeBuffer2, mSaveTextureBuffer2);
            runAll(mRunOnDrawEnd);
        }
        else
        {
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
            mImageFilter.onDraw(mImageFilter.getFrameBufferTexture(), mGLCubeBuffer2, mSaveTextureBuffer2);
            //int tempTextureId; //方法二,有闪烁错误
            //tempTextureId = addPicture.onDrawPicture();
            //mImageFilter.onDraw(tempTextureId, mGLCubeBuffer, mGLTextureBuffer);
            runAll(mRunOnDrawEnd);
        }

        if(save_flag)
        {
            Log.e("onPictureTaken"," "+mConvertedTextureIdForSave);
            int tempTexture2 = mDrawFilter.onDrawPicture();
            float[] textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
            mGLTextureBuffer.put(textureCords);
            mImageFilter2.onDrawFrameBuffer(mConvertedTextureIdForSave, mGLCubeBuffer, mGLTextureBuffer);
            mImageFilter2.onDrawFrameBuffer(tempTexture2, mPictureBuffer, mSaveTextureBuffer);
            Log.e("save_flag:",""+save_flag);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mImageFilter2.getFrameBufferId());
            bitmapsave = saveChanges();
            // addPicture.setPicture(bitmapsave);
            File file = new File("/storage/emulated/0/liwei");
            File file2 = new File("/storage/emulated/0/liwei/1.jpg");
            file.mkdirs();
            try{
                file2.createNewFile();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
            //YuvImage image = new YuvImage(data, ImageFormat.NV21,1920,1080,null);
            try{
                //image.compressToJpeg(new Rect(0,0,1920,1080),50,new FileOutputStream(file2));
                bitmapsave.compress(Bitmap.CompressFormat.JPEG,100,new FileOutputStream(file2));
            }catch(IOException e)
            {

            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            draw_flag = false;
            save_flag = false;
            //releaseCamera();
        }
    }
    public void set_Crop()
    {
        if(textureCropFlag)
        {
            //mGLCubeBuffer2.rewind();
            mGLCubeBuffer2.put(CUBE_CROP).position(0);
            mSaveTextureBuffer2.put(TEXTURE_SAVE_CROP).position(0);
        }
        else
        {
            mGLCubeBuffer2.put(CUBE).position(0);
            mSaveTextureBuffer2.put(TEXTURE_SAVE).position(0);
        }
    }
    public Bitmap saveChanges()
    {
        int width = 1080;
        int height = 1920;

        int size = width * height;
        ByteBuffer buf = ByteBuffer.allocateDirect(size * 4);
        buf.order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buf);

        int data[] = new int[size];
        buf.asIntBuffer().get(data);
        buf = null;
        Bitmap createdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        createdBitmap.setPixels(data, size-width, -width, 0, 0, width, height);
        data = null;

        short sdata[] = new short[size];
        ShortBuffer sbuf = ShortBuffer.wrap(sdata);
        createdBitmap.copyPixelsToBuffer(sbuf);
        for (int i = 0; i < size; ++i) {
            //BGR-565 to RGB-565
            short v = sdata[i];
            sdata[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
        }

        sbuf.rewind();
        createdBitmap.copyPixelsFromBuffer(sbuf);
        return createdBitmap;
    }
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
       // Log.e("data.length",""+data.length);
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
        final int tempWidth = mImageWidth;
        final int tempHeight = mImageHeight;
        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    //Log.e("In onpreviewframe"," "+mImageWidth+mImageHeight);

                    mConvertedTextureId = mImageConvertor.convert(data, tempWidth, tempHeight);
                    //Log.e("data.length",""+data.length);
                    camera.addCallbackBuffer(data);
                }
            });
        }
        else {
            camera.addCallbackBuffer(data);;
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
        final Camera.Size picturesize = camera.getParameters().getPictureSize();
        mImageWidth = size.width;
        mImageHeight = size.height;
        mPictureWidth = picturesize.width;
        mPictureHeight = picturesize.height;
        Log.e("mPictureWidth",""+mPictureWidth);
        Log.e("mPictureHeight",""+mPictureHeight);
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
                if(!releaseFlag)
                {
                    setupSurfaceTexture(camera);
                    camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                    camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                    camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                    camera.setPreviewCallbackWithBuffer(GPUImageImpl.this);
                    camera.startPreview();
                }
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
        releaseFlag = false;
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
    @Override
    public void save()
    {
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken( byte[] data, Camera camera) {


            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                Log.e("data.length",""+data.length);
                runOnDraw(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File("/storage/emulated/0/liwei");
                        File file2 = new File("/storage/emulated/0/liwei/2.jpg");
                        file.mkdirs();
                        try{
                            file2.createNewFile();
                            FileOutputStream fileout = new FileOutputStream(file2);
                            fileout.write(data);
                        }catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                        //YuvImage image = new YuvImage(data, ImageFormat.NV21,1920,1080,null);
                        Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
                        mConvertedTextureIdForSave = OpenGlUtils.loadTexture(picture, OpenGlUtils.NO_TEXTURE);
                        Log.e("mConvertedTextureIdheh",""+mConvertedTextureIdForSave);
                        save_flag = true;
                        draw_flag = true;
                    }
                });
            }
        });

    }
    @Override
    public void restart()
    {
        save_flag = false;
        draw_flag = true;
    }
    @Override
    public void crop()
    {
        textureCropFlag = !textureCropFlag;
        Log.e("In crop","true------------");
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
    @Override
    public void change_Resoluton()
    {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        mImageWidth = previewSize.width;
        mImageHeight = previewSize.height;
        textureFlag = !textureFlag;
        adjustImageScaling();
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
        String s=" ";
        for(int i=0;i<8;i++)
            s+=textureCords[i]+" ";
        Log.e("textureCord",s);
        String s1=" ";
        for(int i=0;i<8;i++)
            s1+=cube[i]+" ";
        Log.e("cube",s1);

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }
    public void releaseCamera()
    {
        if(!releaseFlag)
        {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            releaseFlag = true;
            mCamera = null;
        }
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

    public float[] pixeltotexturecoor(Rect rect)
    {

        float [] coordinate ={transfer(rect.left,rect.top)[0],transfer(rect.left,rect.top)[1],
                        transfer(rect.left,rect.bottom)[0],transfer(rect.left,rect.bottom)[1],
                        transfer(rect.right,rect.bottom)[0],transfer(rect.right,rect.bottom)[1],
                            transfer(rect.right,rect.top)[0],transfer(rect.right,rect.top)[1],
                            transfer(rect.left,rect.top)[0],transfer(rect.left,rect.top)[1]};

        return coordinate;
    }

    public float[] transfer(int x, int y)   //pixel to (-1,1)texture coordinate
    {
        float [] coordinate1 = {-(float)y/1000,(float)x/1000};
        return coordinate1;
    }

    public enum ScaleType { CENTER_INSIDE, CENTER_CROP }

}
