package com.example.liyongjian.mycamear;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.util.Timer;
import java.util.TimerTask;

public class CameraFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener,View.OnTouchListener{

    private final static String TAG = "MainActivity";

    private final int MAX_TIME = 100;// 10s  mTimer设置的每100ms执行一次，所以执行100次是10s

    private boolean isRecorder = false;

    private ImageView startBtn;
    private CircleProgressBar progress;
    private TextView tv;

    private Timer mTimer;// 计时器
    TimerTask timerTask;
    private int mTimeCount;// 时间计数

    private Activity mActivity;

    private SoundPool mSoundPool;

    private int mVideoBeep;

    private int mPhotoBeep;

    public static CameraFragment newInstance() {

        Bundle args = new Bundle();

        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camear,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        startBtn = view.findViewById(R.id.startBtn);
        progress = view.findViewById(R.id.progress);
        tv = view.findViewById(R.id.tv);

        startBtn.setOnClickListener(this);
        startBtn.setOnLongClickListener(this);
        startBtn.setOnTouchListener(this);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        initSoundRes();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseSoundRes();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.startBtn:
                isRecorder = false;
                checkAndPlayShutterdSound();
                Log.d(TAG, "onClick");
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()){
            case R.id.startBtn:
                Log.d(TAG, "onLongClick");

                startRecorder();
                break;
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {


        switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        Log.d(TAG, "ACTION_DOWN");
//                        break;
            case MotionEvent.ACTION_UP:
//                switch (v.getId()){
//                    case R.id.startBtn:
                        if (isRecorder){
                            stopRecorder();
                            Log.d(TAG, "ACTION_UP");
                        }
//                        break;
//                }

                break;
        }
        return false;
    }

    public void startRecorder(){
        checkAndPlayVideoRecordSound();
        isRecorder = true;
        mTimeCount = 0;
        mTimer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                mTimeCount++;

                if (mActivity != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            tv.setText("   " + mTimeCount / 10);
                        }
                    });
                }

                progress.setProgress(mTimeCount);

                if (mTimeCount == MAX_TIME){
                    stopRecorder();
                }
//                Log.d(TAG,"mTimeCount = " + (mTimeCount * 10) % 1000);
//                Log.d(TAG,"mTimeCount2 = " + (mTimeCount * 10) / 1000);
            }
        };

        mTimer.schedule(timerTask, 0, 100);
    }

    public void stopRecorder(){
        checkAndPlayVideoRecordSound();
        isRecorder = false;
        progress.setProgress(0);
        if(timerTask != null)
            timerTask.cancel();
        if (mTimer != null)
            mTimer.cancel();
    }

    private void initSoundRes() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }
        mVideoBeep = mSoundPool.load(mActivity, R.raw.video_record,1);
        mPhotoBeep = mSoundPool.load(mActivity, R.raw.camera_click,1);
    }

    public void checkAndPlayVideoRecordSound() {
        if ( mSoundPool != null) {
            if (mVideoBeep != 0) {
                mSoundPool.play(mVideoBeep, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    public void checkAndPlayShutterdSound() {
        if ( mSoundPool != null) {
            if (mPhotoBeep != 0) {
                mSoundPool.play(mPhotoBeep, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    private void releaseSoundRes() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }
}
