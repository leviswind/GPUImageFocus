package com.wuta.demo.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
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

    private CameraLoaderImpl() {}

    @Override
    public void onResume(Activity activity, IGPUImage image) {
        switchCamera(activity, image);
//        setUpCamera(mCurrentCameraId, activity, image);
    }

    @Override
    public void onPause() {
        releaseCamera();
    }

    @Override
    public void switchCamera(Activity activity, IGPUImage image) {
        releaseCamera();
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
        Camera.Size size = supportedSize.get(0);
        parameters.setPreviewSize(size.width,size.height);
//        parameters.setPreviewFpsRange(30000, 30000);
//        parameters.setPreviewFrameRate(30);
//        parameters.setPreviewFormat(ImageFormat.NV21);
        mCameraInstance.setParameters(parameters);

        debug(mCameraInstance);


        int orientation = mCameraHelper.getCameraDisplayOrientation(activity, mCurrentCameraId);
        ICameraHelper.CameraInfo2 cameraInfo = mCameraHelper.getCameraInfo(mCurrentCameraId);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;

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
        if (supportedSize != null) {
            for (Camera.Size ss : supportedSize) {
                Log.e("Camera", "Support Size: " + "(" + ss.width + ", " + ss.height);
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

    private void releaseCamera() {
        if (mCameraInstance == null) {
            return;
        }
        mCameraInstance.setPreviewCallback(null);
        mCameraInstance.release();
        mCameraInstance = null;
    }
}
