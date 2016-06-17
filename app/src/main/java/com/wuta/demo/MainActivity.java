package com.wuta.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

import com.wuta.demo.camera.CameraLoaderImpl;
import com.wuta.demo.camera.ICameraLoader;
import com.wuta.gpuimage.GPUImage;
import com.wuta.gpuimage.GPUImageFilter;
import com.wuta.gpuimage.GPUImageImpl;
import com.wuta.gpuimage.IGPUImage;
import com.wuta.gpuimage.convert.GPUImageConvertor;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter2;
import com.wuta.gpuimage.exfilters.GPUImageSampleFilter;
import com.wuta.gpuimage.exfilters.GPUImageSwirlFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnTouchListener
{

//    private GPUImage mGPUImage;
    private ICameraLoader mCameraLoader;
    private GPUImageFilter mFilter;
    private GPUImageDrawFilter mDrawFilter;

   // private GPUImageDrawFilter2 mDrawFilter2;

    private IGPUImage mIGPUImage;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
//                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        GLSurfaceView view = (GLSurfaceView) findViewById(R.id.surfaceView);
        mIGPUImage = new GPUImageImpl(this, view
        );//, GPUImageConvertor.ConvertType.SURFACE_TEXTURE);
//        mFilter = new GPUImageSampleFilter();
//        mGPUImage = new GPUImage(this, (GLSurfaceView) findViewById(R.id.surfaceView));
//        mGPUImage.setFilter(mFilter);
        view.setOnTouchListener(this);
        mFilter = new GPUImageSwirlFilter();

        mDrawFilter = new GPUImageDrawFilter();
       // mDrawFilter2 = new GPUImageDrawFilter2();
//        mIGPUImage.setFilter(mFilter);
       // mIGPUImage.setDrawFilter(mDrawFilter);
       // mIGPUImage.setDrawFilter2(mDrawFilter2);

       Bitmap picture = BitmapFactory.decodeResource(getResources(), R.mipmap.testpic);
        Log.e("setPicture","in MainActivity1");
        //mIGPUImage.setDrawPicture(picture);
        mIGPUImage.setBitmap(picture);
        Log.e("setPicture","in MainActivity2");

        mCameraLoader = CameraLoaderImpl.getInstance();
        findViewById(R.id.switcher).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraLoader.switchCamera(MainActivity.this, mIGPUImage);
            }
        });
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // mCameraLoader.save(MainActivity.this,mIGPUImage);
                mIGPUImage.save();
            }
        });
        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraLoader.restart(MainActivity.this,mIGPUImage);
                mIGPUImage.restart();
            }
        });
        findViewById(R.id.crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIGPUImage.crop();
            }
        });
        findViewById(R.id.Resolution).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraLoader.change_Resolution(MainActivity.this,mIGPUImage);
                mIGPUImage.change_Resoluton();
            }
        });
        findViewById(R.id.takephoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera camera = mCameraLoader.getCamera();
                camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        File file = new File("/storage/emulated/0/liwei");
                        File file2 = new File("/storage/emulated/0/liwei/1.jpg");
                        file.mkdirs();
                        try{
                            file2.createNewFile();
                        }catch(IOException e)
                        {
                            e.printStackTrace();
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(file2);
                            fos.write(data);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraLoader.onResume(this, mIGPUImage);
    }

    @Override
    protected void onPause() {
        mCameraLoader.onPause(this, mIGPUImage);
        super.onPause();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e("Camera","entering to the onTouch events");
        mIGPUImage.setFocus(event);
        return false;
    }

}
