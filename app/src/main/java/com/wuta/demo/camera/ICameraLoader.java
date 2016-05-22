package com.wuta.demo.camera;

import android.app.Activity;

import com.wuta.gpuimage.GPUImage;
import com.wuta.gpuimage.IGPUImage;

/**
 * Created by kejin
 * on 2016/5/9.
 */
public interface ICameraLoader
{
    void onResume(Activity activity, IGPUImage image);

    void onPause();

    void switchCamera(Activity activity, IGPUImage image);
}
