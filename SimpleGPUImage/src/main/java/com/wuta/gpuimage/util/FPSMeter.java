package com.wuta.gpuimage.util;

import android.util.Log;

import java.util.HashMap;

/**
 * Created by kejin
 * on 2016/5/12.
 */
public class FPSMeter
{
    private static HashMap<String, FPSMeter> mFPSMeterMap = new HashMap<>();
    public static void meter(String counterName)
    {
        FPSMeter meter = mFPSMeterMap.get(counterName);
        if (meter != null) {
            meter.count();
        }
        else {
            meter = new FPSMeter(counterName);
            mFPSMeterMap.put(counterName, meter);
            meter.count();
        }
    }


    public String mName = "FPS";
    public int mFpsCount = -1;
    public long mStartTime = 0;

    public FPSMeter(String name)
    {
        mName = name;
    }

    public void count()
    {
        if (mFpsCount < 0) {
            mFpsCount = 0;
            mStartTime = System.currentTimeMillis();
        }

        long time = System.currentTimeMillis() - mStartTime;
        if (time > 1000) {
            mFpsCount = (int)(mFpsCount*1000f/time);
            Log.e("FPS", mName + " FPS: " + mFpsCount);
            mFpsCount = 0;
            mStartTime = System.currentTimeMillis();
        }
        else {
            mFpsCount += 1;
        }
    }
}
