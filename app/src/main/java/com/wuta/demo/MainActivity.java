package com.wuta.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity implements OnTouchListener,SensorEventListener
{

//    private GPUImage mGPUImage;
    private ICameraLoader mCameraLoader;
    private GPUImageFilter mFilter;
    private GPUImageDrawFilter mDrawFilter;
    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;
    private TextView vX;
    private TextView vY;
    private TextView vZ;
    private TextView v;
    private Button button;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float[] angle = new float[3];

   // private GPUImageDrawFilter2 mDrawFilter2;

    private IGPUImage mIGPUImage;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
//                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        angle[0] = 0;
        angle[1] = 0;
        angle[2] = 0;
        timestamp = 0;
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
        vX = (TextView) findViewById(R.id.vx);
        vY = (TextView)findViewById(R.id.vy);
        vZ = (TextView)findViewById(R.id.vz);
        v = (TextView)findViewById(R.id.v);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ORIENTATION);
        vX.setText("!!!!!!");


    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
//      if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//        {
//       return;
//        }

//      if (timestamp != 0) {
//          final float dT = (event.timestamp - timestamp) * NS2S;
//          angle[0] += event.values[0] * dT * 100;
//          angle[1] += event.values[1] * dT * 100;
//          angle[2] += event.values[2] * dT * 100;
//         }
//         timestamp = event.timestamp;
//
//
//         vX.setText("X: " + Float.toString(angle[0]));
//         vY.setText("Y: " + Float.toString(angle[1]));
//         vZ.setText("Z: " + Float.toString(angle[2]));

//      方向传感器提供三个数据，分别为azimuth、pitch和roll。
//
//      azimuth：方位，返回水平时磁北极和Y轴的夹角，范围为0°至360°。
//      0°=北，90°=东，180°=南，270°=西。
//
//      pitch：x轴和水平面的夹角，范围为-180°至180°。
//      当z轴向y轴转动时，角度为正值。
//
//      roll：y轴和水平面的夹角，由于历史原因，范围为-90°至90°。
//      当x轴向z轴移动时，角度为正值。

        vX.setText("Orientation Z: " + event.values[0]);
        vY.setText("Orientation X: " + event.values[1]);
        vZ.setText("Orientation Y: " + event.values[2]);
        if(event.values[2]>=45)
        {
            v.setText("左倾");
        }
        else if(event.values[2]<=-45)
        {
            v.setText("右倾");
        }
        else if(event.values[1]<25 && event.values[1]>-25)
        {
            v.setText("平放");
        }
        else{
            v.setText("正面");
        }


}

    @Override
    protected void onResume() {
        super.onResume();
        mCameraLoader.onResume(this, mIGPUImage);
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);  //为传感器注册监听器
    }

    @Override
    protected void onPause() {
        mCameraLoader.onPause(this, mIGPUImage);
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e("Camera","entering to the onTouch events");
        mIGPUImage.setFocus(event);
        return false;
    }

}
