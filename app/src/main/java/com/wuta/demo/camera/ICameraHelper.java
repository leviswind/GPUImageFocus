package com.wuta.demo.camera;

import android.app.Activity;
import android.hardware.Camera;

/**
 * Created by Kejin
 * Date: 2016/5/5
 */
public interface ICameraHelper
{
    int getNumberOfCameras();

    Camera openCamera(int id);


    boolean hasFrontCamera();
    Camera openFrontCamera();

    boolean hasBackCamera();
    Camera openBackCamera();

    void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) ;

    int getCameraDisplayOrientation(Activity activity, int cameraId);

    CameraInfo2 getCameraInfo(int cameraId);


    class CameraInfo2 {
        public int facing;
        public int orientation;
    }
}
