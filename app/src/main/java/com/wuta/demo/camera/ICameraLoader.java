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

    void onPause(Activity activity, IGPUImage image);

    void switchCamera(Activity activity, IGPUImage image);

    void change_Resolution(Activity activity, IGPUImage mIgpuImage);

    void releaseCamera(IGPUImage mIgpuImage);

    void restartCamera(Activity activity, IGPUImage image);

    void save(Activity activity, IGPUImage mIgpuImage);

    void restart(Activity activity, IGPUImage mIgpuImage);
}
