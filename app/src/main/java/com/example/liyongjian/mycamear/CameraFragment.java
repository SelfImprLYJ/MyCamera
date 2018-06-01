package com.example.liyongjian.mycamear;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.util.TimeUnit;
import android.media.AudioManager;
import android.media.ImageReader;
import android.media.SoundPool;
import android.net.wifi.aware.Characteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class CameraFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener,View.OnTouchListener{

    private final static String TAG = "CameraFragment";

    private final int MAX_TIME = 100;// 10s  mTimer设置的每100ms执行一次，所以执行100次是10s

    private boolean isRecorder = false;

    private Size mPreviewSize;

    private boolean mFlashSupported;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private ImageView startBtn;
    private CircleProgressBar progress;
    private TextView tv;

    private Timer mTimer;// 计时器
    TimerTask timerTask;
    private int mTimeCount;// 时间计数

    private Activity mActivity;

    private SoundPool mSoundPool;

    private String mCameraId;

    private int mVideoBeep;

    private int mPhotoBeep;

    private boolean isFront;

    private AutoFitTextureView mTextureView;

    private ImageReader mImageReader;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CaptureRequest mPreviewRequest;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Log.d(TAG,"OnImageAvailableListener onImageAvailable");
                }
            };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CameraCaptureSession mCaptureSession;


    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private final static int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    private CameraDevice mCameraDevice ;

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                }
            };

    private final CameraCaptureSession.StateCallback mSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice){
                        return;
                    }

                    mCaptureSession = cameraCaptureSession;

                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE
                                ,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        if (mFlashSupported) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        }

                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest,null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    showToast("onConfigureFailed");
                }
            };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }

        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    mSessionStateCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    openCamera(width,height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                    configureTransform(width, height);
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
        closeCamera();
        releaseSoundRes();
        stopBackgroundThread();
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }finally {
            mCameraOpenCloseLock.release();
        }

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
        width = 1080;
        height= 1920;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            requestCameraPermission();
            return;
        }

        setUpCameraOutputs(width,height);
        configureTransform(width,height);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, java.util.concurrent.TimeUnit.MICROSECONDS)){
                throw new RuntimeException("Timeout waiting to lock camera opening");
            }

            manager.openCamera(mCameraId,mStateCallback,mBackgroundHandler);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        if (activity != null) {
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] camearIds = manager.getCameraIdList();

                Log.d(TAG,"camearIds.length: " + camearIds.length);
                for (String cameraId : camearIds){
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    // We don't use a front facing camera in this sample.
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }

                    StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }

                    // For still image captures, we use the largest available size.
                    Size largest = Collections.max(
                            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                            new CompareSizesByArea());

                    mImageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),
                            ImageFormat.JPEG,2);

                    mImageReader.setOnImageAvailableListener(null,mBackgroundHandler);

                    int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                    /**
                     * Orientation of the camera sensor
                     */

                    mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                    Log.d(TAG,"displayRotation " + displayRotation + ",mSensorOrientation " + mSensorOrientation);

                    boolean swappedDimensions = false;
                    switch (displayRotation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                                swappedDimensions = true;
                            }
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                                swappedDimensions = true;
                            }
                            break;
                        default:
                            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                    }

                    Point displaySize = new Point();
                    activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                    int rotatedPreviewWidth = width;
                    int rotatedPreviewHeight = height;
                    int maxPreviewWidth = displaySize.x;
                    int maxPreviewHeight = displaySize.y;


                    if (swappedDimensions) {
                        rotatedPreviewWidth = height;
                        rotatedPreviewHeight = width;
                        maxPreviewWidth = displaySize.y;
                        maxPreviewHeight = displaySize.x;
                    }

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    }

                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                            maxPreviewHeight, largest);

                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        mTextureView.setAspectRatio(
                                mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    } else {
                        mTextureView.setAspectRatio(
                                mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    }

                    Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    mFlashSupported = available == null ? false : available;

                    mCameraId = cameraId;

                    return;
                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }


    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
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

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
