package com.example.liyongjian.mycamear;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
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

    private AutoFitTextureView mTextureView;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private final static int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    openCamera(width,height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            };


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
        mTextureView = view.findViewById(R.id.texture);

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
        startBackgroundThread();
        initSoundRes();
        if (mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        }else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseSoundRes();
        stopBackgroundThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.startBtn:
                isRecorder = false;
                Log.d(TAG,"take picture");
                checkAndPlayShutterdSound();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()){
            case R.id.startBtn:
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
                        if (isRecorder){
                            stopRecorder();
                        }
                break;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION){
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void openCamera(int width , int height){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            requestCameraPermission();
            return;
        }
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            new ConfirmationDialog().show(getFragmentManager(),FRAGMENT_DIALOG);
        }else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void startRecorder(){
        Log.d(TAG, "startRecorder");

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
        Log.d(TAG, "stopRecorder");
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

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
}
