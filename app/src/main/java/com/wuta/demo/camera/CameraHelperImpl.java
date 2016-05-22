package com.wuta.demo.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

/**
 * Created by Kejin
 * Date: 2016/5/5
 */
public class CameraHelperImpl implements ICameraHelper
{
    private final static ICameraHelper sInstance = new CameraHelperImpl();
    public static ICameraHelper getInstance()
    {
        return sInstance;
    }

    private CameraHelperImpl() {}

    @Override
    public int getNumberOfCameras()
    {
        return Camera.getNumberOfCameras();
    }

    @Override
    public Camera openCamera(int id)
    {
        return Camera.open(id);
    }

    @Override
    public boolean hasFrontCamera()
    {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT) != -1;
    }

    @Override
    public Camera openFrontCamera()
    {
        int id = getCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        return Camera.open(id);
    }

    @Override
    public boolean hasBackCamera()
    {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK) != -1;
    }

    @Override
    public Camera openBackCamera()
    {
        return Camera.open(getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK));
    }

    @Override
    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera)
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getCameraDisplayOrientation(activity, cameraId);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public int getCameraDisplayOrientation(Activity activity, int cameraId)
    {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        CameraInfo2 info = getCameraInfo(cameraId);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public CameraInfo2 getCameraInfo(int cameraId)
    {
        CameraInfo2 cameraInfo = new CameraInfo2();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        cameraInfo.facing = info.facing;
        cameraInfo.orientation = info.orientation;

        return cameraInfo;
    }


    private int getCameraId(final int facing)
    {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int id = 0; id < numberOfCameras; id++) {
            Camera.getCameraInfo(id, info);
            Log.e("xo", "dd3: " + info.facing + " F: " + facing);
            if (info.facing == facing) {
                Log.e("xo", "dd");
                return id;
            }
        }
        return -1;
    }
}
