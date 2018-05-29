package com.example.administrator.weichatshakedemo;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.lang.ref.WeakReference;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int START_SHAKE = 0x1;
    private static final int AGAIN_SHAKE = 0x2;
    private static final int END_SHAKE = 0x3;
    private int mWeiChatAudio;
    private boolean isShake = false;                                                                // 记录摇动状态

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Vibrator mVibrator;                                                                     // 手机震动
    private SoundPool mSoundPool;                                                                   // 摇一摇音效
    private MyHandler mHandler;
    private GifImageView shakeGifImageView;
    private GifDrawable gifDrawable;
    private CircleImageView shakeCircleImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);                      // 设置只竖屏
        setContentView(R.layout.activity_main2);

        initView();                                                                                 // 初始化View
        mHandler = new MyHandler(this);

        /** 初始化SoundPool */
        mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        mWeiChatAudio = mSoundPool.load(this, R.raw.yisell_sound, 1);

        /** 获取Vibrator震动服务 */
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void initView() {
        shakeCircleImageView = findViewById(R.id.shakeCircleImageView);
        shakeGifImageView = (GifImageView) findViewById(R.id.shakeGifImageView);
        gifDrawable = (GifDrawable) shakeGifImageView.getDrawable();
        gifDrawable.stop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));                      // 获取 SensorManager 负责管理传感器
        if (mSensorManager != null) {
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  // 获取加速度传感器
            if (mAccelerometerSensor != null) {
                mSensorManager.registerListener(
                        this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    /**
     * 务必要在pause中注销 mSensorManager, 否则会造成界面退出后摇一摇依旧生效的bug
     */
    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    /**
     * SensorEventListener回调方法
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_ACCELEROMETER) {

            /** 获取三个方向值 */
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            if ((Math.abs(x) > 17 || Math.abs(y) > 17 || Math.abs(z) > 17) && !isShake) {
                isShake = true;
                /** 实现摇动逻辑, 摇动后进行震动 */
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            /** 开始震动 发出提示音 展示动画效果 */
                            mHandler.obtainMessage(START_SHAKE).sendToTarget();
                            Thread.sleep(500);

                            /** 再来一次震动提示 */
                            mHandler.obtainMessage(AGAIN_SHAKE).sendToTarget();
                            Thread.sleep(500);

                            /** 震動後的收尾 */
                            mHandler.obtainMessage(END_SHAKE).sendToTarget();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class MyHandler extends Handler {

        private WeakReference<MainActivity> mReference;
        private MainActivity mActivity;

        public MyHandler(MainActivity activity) {
            /** 使用弱引用(WeakReference) 防止内存泄露 */
            mReference = new WeakReference<MainActivity>(activity);
            if (mReference != null) {
                mActivity = mReference.get();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_SHAKE:
                    mActivity.mVibrator.vibrate(300);                                  // 需要申請權限, VIBRATE
                    mActivity.mSoundPool.play(                                                      // 发出提示音
                            mActivity.mWeiChatAudio,
                            1, 1, 0, 0, 1);
                    shakeGifImageView.setImageResource(R.drawable.gudetama2);
                    gifDrawable = (GifDrawable) shakeGifImageView.getDrawable();
                    gifDrawable.start();
                    gifDrawable.setLoopCount(1);
                    YoYo.with(Techniques.Shake)
                            .duration(500)
                            .repeat(0)
                            .playOn(shakeCircleImageView);
                    break;
                case AGAIN_SHAKE:
                    mActivity.mVibrator.vibrate(300);
                    break;
                case END_SHAKE:
                    mActivity.isShake = false;                                                      // 整体效果结束, 将震动设置为false
                    shakeGifImageView.setImageResource(R.drawable.gudetama3);
                    gifDrawable = (GifDrawable) shakeGifImageView.getDrawable();
                    gifDrawable.stop();
                    break;
            }
        }
    }
}
