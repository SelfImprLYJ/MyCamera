package com.example.liyongjian.mycamear;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity  {

    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }
//    private final static String TAG = "MainActivity";
//
//    private final int MAX_TIME = 100;// 10s  mTimer设置的每100ms执行一次，所以执行100次是10s
//
//    private boolean isRecorder = false;
//
//    private ImageView startBtn;
//    private CircleProgressBar progress;
//
//    private Timer mTimer;// 计时器
//    TimerTask timerTask;
//    private int mTimeCount;// 时间计数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_camera,CameraFragment.newInstance())
                    .commitAllowingStateLoss();
        }

//        startBtn = findViewById(R.id.startBtn);
//        progress = findViewById(R.id.progress);
//
//        startBtn.setOnClickListener(this);
//        startBtn.setOnLongClickListener(this);

//        startBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
////                    case MotionEvent.ACTION_DOWN:
////                        Log.d(TAG, "ACTION_DOWN");
////                        break;
//                    case MotionEvent.ACTION_UP:
//                        if (isRecorder){
//                            stopRecorder();
//                            Log.d(TAG, "ACTION_UP");
//                        }
//                        break;
//                }
//
//                return false;
//            }
//        });

    }

//    @Override
//    public void onClick(View v) {
//        isRecorder = false;
//        Log.d(TAG, "onClick");
//    }
//
//    @Override
//    public boolean onLongClick(View v) {
//
//
//        return true;
//    }


//
//    public void startRecorder(){
//        isRecorder = true;
//        mTimeCount = 0;
//        mTimer = new Timer();
//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                mTimeCount++;
//                progress.setProgress(mTimeCount);
//
//                if (mTimeCount == MAX_TIME){
//                    stopRecorder();
//                }
//                Log.d(TAG,"mTimeCount = " + mTimeCount);
//            }
//        };
//
//        mTimer.schedule(timerTask, 0, 100);
//    }
//
//    public void stopRecorder(){
//
//        isRecorder = false;
//        progress.setProgress(0);
//        if(timerTask != null)
//            timerTask.cancel();
//        if (mTimer != null)
//            mTimer.cancel();
//    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();
}
