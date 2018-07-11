package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.StreamUtils;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.PreviewAppearance;

/**
 *  演示横屏 PK 模式下的副主播端代码
 *  PK 模式只支持一个主播和一个副主播进行连麦，布局配置方法如下：
 *  主播本地预览设置为全屏，`PreviewAppearance`设置为左半屏幕，然后上面叠加一个屏幕，设置为右边一半，用于显示副主播的画面
 *  主播需要配置合流画面的参数：把副主播的窗口占比50%，位于右边
 *  副主播本地预览设置为右半屏，然后上面叠加一个屏幕，设置为全屏，用于显示主播的画面
 */
public class PKViceAnchorActivity extends AppCompatActivity {
    private static final String TAG = "PKViceAnchorActivity";

    private Button mControlButton;
    private CheckBox mMuteCheckBox;

    private ProgressDialog mProgressDialog;

    private RTCMediaStreamingManager mRTCStreamingManager;

    private boolean mIsActivityPaused = true;
    private boolean mIsConferenceStarted = false;
    private int mCurrentCamFacingIndex;

    private String mRoomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pk_vice_anchor);

        /**
         * Step 1: init sdk, you can also move this to Application.onCreate
         */
        RTCMediaStreamingManager.init(getApplicationContext());

        GLSurfaceView cameraPreviewFrameView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        /**
         * Must bring the camera preview frame to front
         */
        cameraPreviewFrameView.setZOrderOnTop(true);
        cameraPreviewFrameView.setZOrderMediaOverlay(true);
        cameraPreviewFrameView.bringToFront();

        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        mRoomName = getIntent().getStringExtra("roomName");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        /**
         * Step 2: find & init views
         */
        mControlButton = (Button) findViewById(R.id.ControlButton);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);
        mControlButton.setText("开始连麦");

        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        /**
         * Step 3: config camera settings
         */
        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_4_3)
                .setBuiltInFaceBeautyEnabled(true) // Using sdk built in face beauty algorithm
                .setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)) // sdk built in face beauty settings
                .setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off

        /**
         * Step 4: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mRTCStreamingManager = new RTCMediaStreamingManager(getApplicationContext(), cameraPreviewFrameView, codecType);
        mRTCStreamingManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCStreamingManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCStreamingManager.setDebugLoggingEnabled(false);

        /**
         * Step 5: set conference options
         */
        RTCConferenceOptions options = new RTCConferenceOptions();
        // RATIO_4_3 & VIDEO_ENCODING_SIZE_HEIGHT_240 means the output size is 640 x 480
        options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_4_3);
        options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_480);
        // vice anchor can use a higher conference bitrate for better image quality
        options.setVideoBitrateRange(300 * 1000, 800 * 1000);
        // 20 fps is enough
        options.setVideoEncodingFps(20);
        mRTCStreamingManager.setConferenceOptions(options);

        /**
         * Step 6: create the remote windows
         */
        RTCVideoWindow windowA = new RTCVideoWindow((GLSurfaceView)findViewById(R.id.RemoteGLSurfaceViewA));

        /**
         * Step 7: add the remote windows
         */
        mRTCStreamingManager.addRemoteWindow(windowA);

        /**
         * Set the Camera Preview Appearance
         *
         * left-top is origin, pointing right/down is positive
         * @param x percentage x [0.0f-1.0f] in GLSurfaceView
         * @param y percentage y [0.0f-1.0f] in GLSurfaceView
         * @param w percentage width [0.0f-1.0f] of GLSurfaceView
         * @param h percentage height [0.0f-1.0f] of GLSurfaceView
         * @param scaleType the scale type of preview
         */
        PreviewAppearance appearance = new PreviewAppearance(PreviewAppearance.ScaleType.FULL);

        /**
         * Step 8: do prepare
         */
        mRTCStreamingManager.prepare(cameraStreamingSetting, null, null, null, appearance);

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        /**
         * Step 9: You must start capture before conference or streaming
         * You will receive `Ready` state callback when capture started success
         */
        mRTCStreamingManager.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityPaused = true;
        /**
         * Step 10: You must stop capture, stop conference, stop streaming when activity paused
         */
        mRTCStreamingManager.stopCapture();
        stopConference();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 11: You must call destroy to release some resources when activity destroyed
         */
        mRTCStreamingManager.destroy();
        /**
         * Step 12: You can also move this to your MainActivity.onDestroy
         */
        RTCMediaStreamingManager.deinit();
    }

    public void onClickSwitchCamera(View v) {
        mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
        CameraStreamingSetting.CAMERA_FACING_ID facingId;
        if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        }
        Log.i(TAG, "switchCamera:" + facingId);
        mRTCStreamingManager.switchCamera(facingId);
    }

    public void onClickExit(View v) {
        finish();
    }

    private boolean startConference() {
        if (mIsConferenceStarted) {
            return true;
        }
        mProgressDialog.setMessage("正在加入连麦 ... ");
        mProgressDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                startConferenceInternal();
            }
        });
        return true;
    }

    private boolean startConferenceInternal() {
        String roomToken = StreamUtils.requestRoomToken(StreamUtils.getTestUserId(this), mRoomName);
        if (roomToken == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息 !", Toast.LENGTH_SHORT);
            return false;
        }
        mRTCStreamingManager.startConference(StreamUtils.getTestUserId(this), mRoomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                dismissProgressDialog();
                showToast(getString(R.string.start_conference), Toast.LENGTH_SHORT);
                updateControlButtonText();
                mIsConferenceStarted = true;
                /**
                 * Because `startConference` is called in child thread
                 * So we should check if the activity paused.
                 */
                if (mIsActivityPaused) {
                    stopConference();
                }
            }

            @Override
            public void onStartConferenceFailed(int errorCode) {
                dismissProgressDialog();
                showToast(getString(R.string.failed_to_start_conference) + errorCode, Toast.LENGTH_SHORT);
            }
        });
        return true;
    }

    private boolean stopConference() {
        if (!mIsConferenceStarted) {
            return true;
        }
        mRTCStreamingManager.stopConference();
        mIsConferenceStarted = false;
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
                    // You must `StartConference` after `Ready`
                    showToast(getString(R.string.ready), Toast.LENGTH_SHORT);
                    break;
                case CONNECT_FAIL:
                    showToast(getString(R.string.failed_to_connect_rtc_server), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_FAILED:
                case AUDIO_PUBLISH_FAILED:
                    showToast(getString(R.string.failed_to_publish_av_to_rtc) + extra, Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_video_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_audio_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case USER_JOINED_AGAIN:
                    showToast(getString(R.string.user_join_other_where), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case USER_KICKOUT_BY_HOST:
                    showToast(getString(R.string.user_kickout_by_host), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case OPEN_CAMERA_FAIL:
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                default:
                    return;
            }
        }
    };

    private RTCRemoteWindowEventListener mRTCRemoteWindowEventListener = new RTCRemoteWindowEventListener() {
        @Override
        public void onRemoteWindowAttached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowAttached: " + remoteUserId);
        }

        @Override
        public void onRemoteWindowDetached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowDetached: " + remoteUserId);
        }
    };

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRTCStreamingManager.mute(mMuteCheckBox.isChecked());
        }
    };
    
    public void onClickStreaming(View v) {
        if (!mIsConferenceStarted) {
            startConference();
        } else {
            stopConference();
        }
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsConferenceStarted) {
                    mControlButton.setText(getString(R.string.stop_conference));
                } else {
                    mControlButton.setText(getString(R.string.start_conference));
                }
            }
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, text, duration).show();
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PKViceAnchorActivity.this, text, duration).show();
            }
        });
    }

    private CameraStreamingSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }
}
