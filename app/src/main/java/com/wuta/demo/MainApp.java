package com.wuta.demo;

import android.app.Application;

/**
 * Created by kejin
 * on 2016/5/9.
 */
public class MainApp extends Application
{
    public static MainApp instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }
}
