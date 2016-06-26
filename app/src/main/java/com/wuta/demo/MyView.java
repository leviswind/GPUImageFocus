package com.wuta.demo;

/**
 * Created by LW on 2016/6/26.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MyView extends View {
    private Paint mPaint;
    private Context mContext;
    private Boolean visible = false;
    private float x_bias = .0f;
    private float y_bias = .0f;
    private float x = 100.0f;
    private float y = 200.0f;
    Thread thread ;
    Handler handler;
    public MyView(Context context) {
        super(context);
        mPaint = new Paint();
    }
    public MyView(Context context,AttributeSet attrs)
    {
        super(context,attrs);
        mPaint = new Paint();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MyView);
        int textColor = a.getColor(R.styleable.MyView_textColor,
                0XFFFFFFFF);
        float textSize = a.getDimension(R.styleable.MyView_textSize, 20);

        mPaint.setTextSize(textSize);
        mPaint.setColor(textColor);
        a.recycle();
        handler= new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle b = msg.getData();
                boolean flag = b.getBoolean("flag");
                if(flag) {
                    visible = false;
                }
            }
        };
        thread = new Thread(new Runnable(){
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.e("in thread","failed");
                }
                Message msg = new Message();
                Bundle b = new Bundle();
                Log.e("in thread","success");
                b.putBoolean("flag",true);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        });
    }
    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        if(visible)
        {
            //设置填充
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x-40,y-y_bias-10,x+40,y-y_bias+10,mPaint);
            canvas.drawRect(x-10,y-200,x+10,y+200,mPaint);
        }
        invalidate();
    }
    public float get_Ybias()
    {
        return y_bias;
    }
    public void setInvisible()
    {
        thread.interrupt();
        thread = new Thread(new Runnable(){
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.e("in thread","failed");
                    return;
                }
                Message msg = new Message();
                Bundle b = new Bundle();
                Log.e("in thread","success");
                b.putBoolean("flag",true);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        });
        thread.start();
    };
    public void setXY(float x1, float y1){
        x = x1;
        y = y1;
        x_bias = .0f;
        y_bias = .0f;
    }
    public void setX(float x)
    {
        x_bias =x_bias+x;
    }
    public void setY(float y)
    {

        y_bias =y_bias+y;
        if(y_bias>=200)
        {
            y_bias = 200;
        }else if(y_bias<=-200)
        {
            y_bias = -200;
        }
    }
    public void setVisible()
    {
        visible = !visible;
    }
    public void setVisible(Boolean bool)
    {
        visible = bool;
    }
}