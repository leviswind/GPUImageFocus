package com.wuta.demo.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.wuta.gpuimage.GPUImage;
import com.wuta.gpuimage.IGPUImage;

import java.util.List;

/**
 * Created by kejin
 * on 2016/5/9.
 */
public class CameraLoaderImpl implements ICameraLoader
{
    private int mCurrentCameraId = 0;
    private Camera mCameraInstance;

    private ICameraHelper mCameraHelper = CameraHelperImpl.getInstance();

    private static ICameraLoader mInstance = null;
    public static ICameraLoader getInstance()
    {
        if (mInstance == null) {
            mInstance = new CameraLoaderImpl();
        }

        return mInstance;
    }

    @Override
    public Camera getCamera() {
        return mCameraInstance;
    }

    private CameraLoaderImpl() {}

    @Override
    public void onResume(Activity activity, IGPUImage image) {
        restartCamera(activity, image);
//        setUpCamera(mCurrentCameraId, activity, image);
    }

    @Override
    public void onPause(Activity activity, IGPUImage image) {
        releaseCamera(image);
    }

    @Override
    public void switchCamera(Activity activity, IGPUImage image) {
        releaseCamera(image);
        mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
        setUpCamera(mCurrentCameraId, activity, image);
    }

    private void setUpCamera(final int id, Activity activity, IGPUImage image) {
        mCameraInstance = getCameraInstance(id);
        if (mCameraInstance == null) {
            return;
        }
//        mCameraHelper.setCameraDisplayOrientation(activity, id, mCameraInstance);
        Camera.Parameters parameters = mCameraInstance.getParameters();
        // TODO adjust by getting supportedPreviewSizes and then choosing
        // the best one for screen size (best fill screen)
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
//        parameters.set("fast-fps-mode", 1);
        List<Camera.Size> supportedSize = parameters.getSupportedPreviewSizes();
//        parameters.setRecordingHint(true);
        Camera.Size size = parameters.getPreviewSize();
        Log.e("in setUpCamera: size is"," "+size.width+" "+size.height);
        parameters.setPreviewSize(640, 480);
//        parameters.setPreviewFpsRange(30000, 30000);
//        parameters.setPreviewFrameRate(30);
//        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPictureSize(1920,1080);
        parameters.setPictureFormat(ImageFormat.JPEG);
        mCameraInstance.setParameters(parameters);

        debug(mCameraInstance);


        int orientation = mCameraHelper.getCameraDisplayOrientation(activity, mCurrentCameraId);
        ICameraHelper.CameraInfo2 cameraInfo = mCameraHelper.getCameraInfo(mCurrentCameraId);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;

        image.setupCamera(mCameraInstance, orientation, false, flipHorizontal);
    }
    private void setUpCamera(Activity activity, IGPUImage image)
    {
        Camera.Parameters parameters = mCameraInstance.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        Log.e("in setUpCamera: size is"," "+size.width+" "+size.height);
        int orientation = mCameraHelper.getCameraDisplayOrientation(activity, mCurrentCameraId);
        ICameraHelper.CameraInfo2 cameraInfo = mCameraHelper.getCameraInfo(mCurrentCameraId);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraInstance.release();
        mCameraInstance=null;
        mCameraInstance = getCameraInstance(mCurrentCameraId);
        mCameraInstance.setParameters(parameters);
        image.setupCamera(mCameraInstance, orientation, false, flipHorizontal);
    }

    private void debug(Camera camera)
    {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        Log.e("camera", "Size: " + size.width + ", " + size.height);


        List<int []> ranges = parameters.getSupportedPreviewFpsRange();
        Log.e("Camera", "supported fps size: " + (ranges== null ? 0 : ranges.size()));
        if (ranges != null) {
            for (int[] r : ranges) {
                Log.e("Camera", "Support: (" + r[0] + ", " + r[1] + ")");
            }
        }

        List<Camera.Size> supportedSize = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedCameraSize = parameters.getSupportedPictureSizes();
        if (supportedSize != null) {
            for (Camera.Size ss : supportedSize) {
                Log.e("Camera", "Support Size: " + "(" + ss.width + ", " + ss.height);
            }
        }
        if (supportedCameraSize != null) {
        for (Camera.Size ss : supportedCameraSize) {
            Log.e("Camera", "Support Picture Size: " + "(" + ss.width + ", " + ss.height);
        }
    }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(final int id) {
        Camera c = null;
        try {
            c = mCameraHelper.openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }
    private boolean resolutionFlag = true;
    @Override
    public void change_Resolution(Activity activity, IGPUImage mIgpuImage)
    {
        if(resolutionFlag && mCameraInstance!=null)
        {
            releaseCamera(mIgpuImage);
            mCameraInstance = getCameraInstance(mCurrentCameraId);
            Camera.Parameters parameters = mCameraInstance.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            Log.e("setUpCamera out first"," "+size.width+" "+size.height);
            parameters.setPreviewSize(480, 320);
            mCameraInstance.setParameters(parameters);
            Camera.Parameters parameters2 = mCameraInstance.getParameters();
            Camera.Size size2 = parameters.getPreviewSize();
            Log.e("setUpCamera out second"," "+size2.width+" "+size2.height);
            //mCameraInstance.setPreviewCallback(null);
            //mCameraInstance.release();
            setUpCamera(activity, mIgpuImage);
        }
        else if(mCameraInstance!=null)
        {
            releaseCamera(mIgpuImage);
            mCameraInstance = getCameraInstance(mCurrentCameraId);
            Camera.Parameters parameters = mCameraInstance.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            Log.e("setUpCamera out first"," "+size.width+" "+size.height);
            parameters.setPreviewSize(1920, 1080);
            mCameraInstance.setParameters(parameters);
            Camera.Parameters parameters2 = mCameraInstance.getParameters();
            Camera.Size size2 = parameters.getPreviewSize();
            Log.e("setUpCamera out second"," "+size2.width+" "+size2.height);
            //mCameraInstance.setPreviewCallback(null);
            //mCameraInstance.release();
            setUpCamera(activity, mIgpuImage);
        }
        resolutionFlag = !resolutionFlag;
    }
    @Override
    public void save(Activity activity, IGPUImage mIgpuImage)
    {
        releaseCamera(mIgpuImage);
        mCameraInstance = getCameraInstance(mCurrentCameraId);
        Camera.Parameters parameters = mCameraInstance.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        Log.e("setUpCamera out first"," "+size.width+" "+size.height);
        parameters.setPreviewSize(1920,1080);
        mCameraInstance.setParameters(parameters);
        Camera.Parameters parameters2 = mCameraInstance.getParameters();
        Camera.Size size2 = parameters.getPreviewSize();
        Log.e("setUpCamera out second"," "+size2.width+" "+size2.height);
        //mCameraInstance.setPreviewCallback(null);
        //mCameraInstance.release();
        setUpCamera(activity, mIgpuImage);
    }
    @Override
    public void restart(Activity activity, IGPUImage mIgpuImage)
    {
        //releaseCamera();
        mCameraInstance = getCameraInstance(mCurrentCameraId);
        Camera.Parameters parameters = mCameraInstance.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        Log.e("setUpCamera out first"," "+size.width+" "+size.height);
        parameters.setPreviewSize(480, 320);
        mCameraInstance.setParameters(parameters);
        Camera.Parameters parameters2 = mCameraInstance.getParameters();
        Camera.Size size2 = parameters.getPreviewSize();
        Log.e("setUpCamera out second"," "+size2.width+" "+size2.height);
        //mCameraInstance.setPreviewCallback(null);
        //mCameraInstance.release();
        setUpCamera(activity, mIgpuImage);
    }
    @Override
    public void releaseCamera(IGPUImage mIgpuImage) {
        if (mCameraInstance == null) {
            return;
        }
        mIgpuImage.releaseCamera();
        mCameraInstance = null;
    }
    @Override
    public void restartCamera(Activity activity, IGPUImage image)
    {
        setUpCamera(mCurrentCameraId, activity, image);
    }

}
