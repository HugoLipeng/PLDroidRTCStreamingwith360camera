package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.widget.PLVideoView;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCFrameCapturedCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCUserEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.StreamUtils;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingPreviewCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.WatermarkSetting;
import com.qiniu.pili.droid.streaming.av.common.PLFourCC;
import com.qiniu.pili.droid.streaming.widget.AspectFrameLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import us.pinguo.pgskinprettifyengine.HomeItemCell;
import us.pinguo.pgskinprettifyengine.IRecycleCell;
import us.pinguo.pgskinprettifyengine.ItemData;
import us.pinguo.pgskinprettifyengine.MyItemClickListener;
import us.pinguo.pgskinprettifyengine.MyRecycleAdapter;
import us.pinguo.pgskinprettifyengine.PGGLContextManager;
import us.pinguo.pgskinprettifyengine.PGSkinPrettifyEngine;

/**
 * 演示使用 SDK 内部的 Video/Audio 采集，实现连麦 & 推流
 */
public class CapStreamingActivity extends AppCompatActivity implements
        StreamingPreviewCallback,
        SeekBar.OnSeekBarChangeListener,
        SurfaceTextureCallback, MyItemClickListener {

    private static final String TAG = "CapStreamingActivity";
    private static final int MESSAGE_ID_RECONNECTING = 0x01;

    private TextView mStatusTextView;
    private TextView mStatTextView;
    private Button mControlButton;
    private CheckBox mMuteCheckBox;
    private CheckBox mConferenceCheckBox;
    private CheckBox cbBeauty;

    private Toast mToast = null;
    private ProgressDialog mProgressDialog;

    private RTCMediaStreamingManager mRTCStreamingManager;

    private StreamingProfile mStreamingProfile;

    private boolean mIsActivityPaused = true;
    private boolean mIsPublishStreamStarted = false;
    private boolean mIsConferenceStarted = false;
    private boolean mIsInReadyState = false;
    private int mCurrentCamFacingIndex;

    private GLSurfaceView mCameraPreviewFrameView;
    private RTCVideoWindow mRTCVideoWindowA;
    private RTCVideoWindow mRTCVideoWindowB;

    private int mRole;
    private String mRoomName;

    private boolean mIsPreviewMirror = false;
    private boolean mIsEncodingMirror = false;

    RTCConferenceOptions options;
    CameraStreamingSetting cameraStreamingSetting;
    WatermarkSetting watermarksetting;
//    String uid;

    boolean port = true;
    PLVideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture_streaming);

        /**
         * Step 1: init sdk, you can also move this to Application.onCreate
         */
        RTCMediaStreamingManager.init(getApplicationContext());

        /**
         * Step 2: find & init views
         */
        AspectFrameLayout afl = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
        afl.setShowMode(AspectFrameLayout.SHOW_MODE.FULL);
        mCameraPreviewFrameView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);

        mRole = getIntent().getIntExtra("role", StreamUtils.RTC_ROLE_VICE_ANCHOR);
//        uid = getIntent().getStringExtra("uid");
        mRoomName = getIntent().getStringExtra("roomName");
        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        boolean isLandscape = getIntent().getBooleanExtra("orientation", false);
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mStatusTextView = (TextView) findViewById(R.id.StatusTextView);
        mStatTextView = (TextView) findViewById(R.id.StatTextView);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);
        mConferenceCheckBox = (CheckBox) findViewById(R.id.ConferenceCheckBox);
        mConferenceCheckBox.setOnClickListener(mConferenceButtonClickListener);

        cbBeauty = (CheckBox) findViewById(R.id.cb_beauty);
        cbBeauty.setOnClickListener(cbBeautyClick);

        if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
            mConferenceCheckBox.setVisibility(View.VISIBLE);
        }

        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        /**
         * Step 3: config camera settings
         */
        cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9)
                .setBuiltInFaceBeautyEnabled(false) // Using sdk built in face beauty algorithm
                .setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)) // sdk built in face beauty settings
                .setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off

        /**
         * Step 4: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mRTCStreamingManager = new RTCMediaStreamingManager(getApplicationContext(), afl, mCameraPreviewFrameView, codecType);
        mRTCStreamingManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCStreamingManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCStreamingManager.setUserEventListener(mRTCUserEventListener);
        mRTCStreamingManager.setDebugLoggingEnabled(true);

//        mCameraPreviewFrameView.setZOrderOnTop(true);
        /**
         * Step 5: set conference options
         */
        options = new RTCConferenceOptions();
        if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
            // anchor should use a bigger size, must equals to `StreamProfile.setPreferredVideoEncodingSize` or `StreamProfile.setEncodingSizeLevel`
            // RATIO_16_9 & VIDEO_ENCODING_SIZE_HEIGHT_480 means the output size is 848 x 480
            options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_16_9);
            options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_480);
            // anchor can use a smaller conference bitrate in order to reserve enough bandwidth for rtmp streaming
            options.setVideoBitrateRange(800 * 1000, 1500 * 1000);
            // 20 fps is enough
            options.setVideoEncodingFps(10);
        } else {
            // vice anchor can use a smaller size
            // RATIO_4_3 & VIDEO_ENCODING_SIZE_HEIGHT_240 means the output size is 320 x 240
            // 4:3 looks better in the mix frame
            options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_4_3);
            options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_240);
            // vice anchor can use a higher conference bitrate for better image quality
            options.setVideoBitrateRange(300 * 1000, 800 * 1000);
            // 20 fps is enough
            options.setVideoEncodingFps(20);
        }
        options.setHWCodecEnabled(!isSwCodec);
        mRTCStreamingManager.setConferenceOptions(options);
        /**
         * Step 6: create the remote windows
         */
        RTCVideoWindow windowA = new RTCVideoWindow(findViewById(R.id.RemoteWindowA), (GLSurfaceView) findViewById(R.id.RemoteGLSurfaceViewA));
        RTCVideoWindow windowB = new RTCVideoWindow(findViewById(R.id.RemoteWindowB), (GLSurfaceView) findViewById(R.id.RemoteGLSurfaceViewB));

        /**
         * Step 7: configure the mix stream position and size (only anchor)
         */
        if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
            // set mix overlay params with absolute value
            // the w & h of remote window equals with or smaller than the vice anchor can reduce cpu consumption
            if (isLandscape) {
                windowA.setAbsolutetMixOverlayRect(options.getVideoEncodingWidth() - 320, 100, 320, 240);
                windowB.setAbsolutetMixOverlayRect(0, 100, 320, 240);
            } else {
                windowA.setAbsolutetMixOverlayRect(options.getVideoEncodingHeight() - 240, 100, 240, 320);
//                windowA.setAbsolutetMixOverlayRect(0, 0, 240, 320);
                windowB.setAbsolutetMixOverlayRect(options.getVideoEncodingHeight() - 240, 420, 240, 320);
            }

            // set mix overlay params with relative value
            // windowA.setRelativeMixOverlayRect(0.65f, 0.2f, 0.3f, 0.3f);
            // windowB.setRelativeMixOverlayRect(0.65f, 0.5f, 0.3f, 0.3f);
        }

        /**
         * Step 8: add the remote windows
         */
        mRTCStreamingManager.addRemoteWindow(windowA);
        mRTCStreamingManager.addRemoteWindow(windowB);


        mRTCVideoWindowA = windowA;
        mRTCVideoWindowB = windowB;

        /**
         * Step 9: do prepare, anchor should config streaming profile first
         */
        if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
            mRTCStreamingManager.setStreamStatusCallback(mStreamStatusCallback);
            mRTCStreamingManager.setStreamingStateListener(mStreamingStateChangedListener);
            mRTCStreamingManager.setStreamingSessionListener(mStreamingSessionListener);

            StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
            StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(20, 1000 * 1024, 48);
            StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);

            mStreamingProfile = new StreamingProfile();
            mStreamingProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_MEDIUM2)
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM1)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.BITRATE_PRIORITY)
                    .setPreferredVideoEncodingSize(options.getVideoEncodingWidth(), options.getVideoEncodingHeight())
//                    .setPreferredVideoEncodingSize(480, 640)
                    .setAdaptiveBitrateEnable(false)
                    .setFpsControllerEnable(true)
                    .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

            //Set AVProfile Manually, which will cover `setXXXQuality`
            //StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
            //StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(30, 1000 * 1024, 48);
            //StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);
            //mStreamingProfile.setAVProfile(avProfile);

            if (isLandscape) {
                mStreamingProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.LAND);
            } else {
                mStreamingProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT);
            }

            watermarksetting = new WatermarkSetting(this);
            watermarksetting.setResourceId(R.drawable.qiniu_logo)
                    .setSize(WatermarkSetting.WATERMARK_SIZE.MEDIUM)
                    .setAlpha(100)
                    .setCustomPosition(0.5f, 0.5f);
            mRTCStreamingManager.prepare(cameraStreamingSetting, null, null, mStreamingProfile);
        } else {
            mControlButton.setText("开始连麦");
            mRTCStreamingManager.prepare(cameraStreamingSetting, null);
        }

        mProgressDialog = new ProgressDialog(this);

//        mRTCStreamingManager.setEncodingMirror(true);

//        mRTCStreamingManager.setDebugLoggingEnabled(true);


        // PG INIT=======================
        m_pPGSkinPrettifyEngine = new PGSkinPrettifyEngine();
        m_bIsFirstFrame = true;
        InitViews();
    }

    // PG INIT VIEWS=======================
    public void InitViews() {
        SeekBar m_Seekpink = (SeekBar) findViewById(R.id.seek_pink);
        m_Seekpink.setOnSeekBarChangeListener(this);
        m_Seekpink.setProgress((int) (mfPinkValue * 100));
        TextView tv_pink = (TextView) findViewById(R.id.pink_value);
        tv_pink.setText(String.valueOf(mfPinkValue));

        SeekBar m_Seekwhiten = (SeekBar) findViewById(R.id.seek_whiten);
        m_Seekwhiten.setOnSeekBarChangeListener(this);
        m_Seekwhiten.setProgress((int) (mfWhitenValue * 100));
        TextView tv_whiten = (TextView) findViewById(R.id.whiten_value);
        tv_whiten.setText(String.valueOf(mfWhitenValue));

        SeekBar m_Seekredden = (SeekBar) findViewById(R.id.seek_redden);
        m_Seekredden.setOnSeekBarChangeListener(this);
        m_Seekredden.setProgress((int) (mfReddenValue * 100));
        TextView tv_redden = (TextView) findViewById(R.id.redden_value);
        tv_redden.setText(String.valueOf(mfReddenValue));


        SeekBar m_Seeksoften = (SeekBar) findViewById(R.id.seek_soften);
        m_Seeksoften.setOnSeekBarChangeListener(this);
        m_Seeksoften.setProgress(mnSoftenValue);
        TextView tv_soften = (TextView) findViewById(R.id.soften_value);
        tv_soften.setText(String.valueOf(mnSoftenValue));

        SeekBar m_SeeksFilter = (SeekBar) findViewById(R.id.filter_redden);
        m_SeeksFilter.setOnSeekBarChangeListener(this);
        m_SeeksFilter.setProgress(mnFilterValue);
        TextView tv_filter = (TextView) findViewById(R.id.filter_value);
        tv_filter.setText(String.valueOf(mnFilterValue));


        Button btn_switch = (Button) findViewById(R.id.btn_switch);
        ((TextView) findViewById(R.id.tv_curAlgorith)).setText("当前：降噪磨皮");
        btn_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mbAlgorith)
                    mbAlgorith = false;
                else
                    mbAlgorith = true;
                if (m_pPGSkinPrettifyEngine == null)
                    return;
                if (mbAlgorith) {
                    ((TextView) findViewById(R.id.tv_curAlgorith)).setText("当前：降噪磨皮");
                    m_pPGSkinPrettifyEngine.SetSkinSoftenAlgorithm(PGSkinPrettifyEngine.PG_SoftenAlgorithm.PG_SoftenAlgorithmDenoise);
                } else {
                    ((TextView) findViewById(R.id.tv_curAlgorith)).setText("当前：细节保留磨皮");
                    m_pPGSkinPrettifyEngine.SetSkinSoftenAlgorithm(PGSkinPrettifyEngine.PG_SoftenAlgorithm.PG_SoftenAlgorithmContrast);
                }
            }
        });

        mListView = (RecyclerView) findViewById(R.id.listview);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mListView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        mListView.setLayoutManager(mLayoutManager);
        ConstructList();
        mAdapter = new MyRecycleAdapter(this, mListData);
        mListView.setAdapter(mAdapter);
        mListView.setItemAnimator(new DefaultItemAnimator());
    }

    private void ConstructList() {
        mListData = new ArrayList<>();
        for (int i = 0; i < mFilterName.length; ++i) {
            ItemData itemData = new ItemData();
            itemData.filterName = mFilterName[i];
            itemData.filterType = mFilterType[i];
            mListData.add(new HomeItemCell(this, itemData, this));
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        /**
         * Step 10: You must start capture before conference or streaming
         * You will receive `Ready` state callback when capture started success
         */

        mRTCStreamingManager.startCapture();

        mRTCStreamingManager.setStreamingPreviewCallback(this);

        mRTCStreamingManager.setSurfaceTextureCallback(this);


        // PG RESUME=======================
        pgSkinResume();
    }

    private void pgSkinResume() {

        if (m_pPGSkinPrettifyEngine == null) {
            m_pPGSkinPrettifyEngine = new PGSkinPrettifyEngine();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        mIsActivityPaused = true;
        /**
         * Step 11: You must stop capture, stop conference, stop streaming when activity paused
         */
        mRTCStreamingManager.stopCapture();
        stopConference();
        stopPublishStreaming();

        pgSkinPause();

    }

    private void pgSkinPause() {
        // 销毁 PGHelixEngine
        if (m_pPGSkinPrettifyEngine != null) {
            Log.i(LOG_TAG, "releasing m_pPGSkinPrettifyEngine");
            m_pPGSkinPrettifyEngine.DestroyEngine();
            m_pPGSkinPrettifyEngine = null;
            m_bIsFirstFrame = true;
        }

        // 销毁 GL Texture
        if (m_pCameraTexture != null) {
            Log.i(LOG_TAG, "releasing camera frame texture");

            m_pCameraTexture.release();
            m_pCameraTexture = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 12: You must call destroy to release some resources when activity destroyed
         */
        mRTCStreamingManager.destroy();
        /**
         * Step 13: You can also move this to your MainActivity.onDestroy
         */
        RTCMediaStreamingManager.deinit();
    }

    public void onClickKickoutUserA(View v) {
        mRTCStreamingManager.kickoutUser(R.id.RemoteGLSurfaceViewA);
    }

    public void onClickKickoutUserB(View v) {
        mRTCStreamingManager.kickoutUser(R.id.RemoteGLSurfaceViewB);
    }

    boolean flag = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("SSSSS", "onConfigurationChanged");
    }

    public void onClickCaptureFrame(View v) {

//        mRTCStreamingManager.mute(true);

//        if (flag) {
//            mVideoView.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mRTCStreamingManager.stopCapture();
//                    mCameraPreviewFrameView.setVisibility(View.INVISIBLE);
//                    mVideoView.setVideoPath("http://ohjxta96r.bkt.clouddn.com/oceans.mp4");
//                    mVideoView.start();
//                }
//            }, 1500);
//        } else {
//            mVideoView.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mRTCStreamingManager.startCapture();
//                    mCameraPreviewFrameView.setVisibility(View.VISIBLE);
//                    mVideoView.pause();
//                    mVideoView.stopPlayback();
//                }
//            }, 1500);
//
//        }
//        flag = !flag;


        mRTCStreamingManager.captureFrame(new RTCFrameCapturedCallback() {
            @Override
            public void onFrameCaptureSuccess(Bitmap bitmap) {
                String filepath = Environment.getExternalStorageDirectory() + "/captured.jpg";
                saveBitmapToSDCard(filepath, bitmap);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filepath)));
                showToast("截帧成功, 存放在 " + filepath, Toast.LENGTH_SHORT);
            }

            @Override
            public void onFrameCaptureFailed(int errorCode) {
                showToast("截帧失败，错误码：" + errorCode, Toast.LENGTH_SHORT);
            }
        });
    }

    public void onClickPreviewMirror(View v) {
        if (mRTCStreamingManager.setPreviewMirror(!mIsPreviewMirror)) {
            mIsPreviewMirror = !mIsPreviewMirror;
            showToast(getString(R.string.mirror_success), Toast.LENGTH_SHORT);
        }
    }

    public void onClickEncodingMirror(View v) {
        if (mRTCStreamingManager.setEncodingMirror(!mIsEncodingMirror)) {
            mIsEncodingMirror = !mIsEncodingMirror;
            showToast(getString(R.string.mirror_success), Toast.LENGTH_SHORT);
        }
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

    public void onClickRemoteWindowA(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mCameraPreviewFrameView.getId()) {
            mRTCStreamingManager.switchRenderView(mCameraPreviewFrameView, mRTCVideoWindowA.getGLSurfaceView());
        } else {
            mRTCStreamingManager.switchRenderView(mRTCVideoWindowA.getGLSurfaceView(), mCameraPreviewFrameView);
        }
    }

    public void onClickRemoteWindowB(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mCameraPreviewFrameView.getId()) {
            mRTCStreamingManager.switchRenderView(mCameraPreviewFrameView, mRTCVideoWindowB.getGLSurfaceView());
        } else {
            mRTCStreamingManager.switchRenderView(mRTCVideoWindowB.getGLSurfaceView(), mCameraPreviewFrameView);
        }
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
//        String roomToken = StreamUtils.requestRoomToken(uid, mRoomName);
        if (roomToken == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息 !", Toast.LENGTH_SHORT);
            return false;
        }
        Log.e("SSSSS", "startConferenceInternal===userID==" + StreamUtils.getTestUserId(this) + "==roomName==" + mRoomName + "==roomToken==" + roomToken);
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
                setConferenceBoxChecked(false);
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
        setConferenceBoxChecked(false);
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    private boolean startPublishStreaming() {
        if (mIsPublishStreamStarted) {
            return true;
        }
        if (!mIsInReadyState) {
            showToast(getString(R.string.stream_state_not_ready), Toast.LENGTH_SHORT);
            return false;
        }
        mProgressDialog.setMessage("正在准备推流... ");
        mProgressDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                startPublishStreamingInternal();
            }
        });
        return true;
    }

    private boolean startPublishStreamingInternal() {
        String publishAddr = StreamUtils.requestPublishAddress(mRoomName);
        if (publishAddr == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息/推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        try {
            if (StreamUtils.IS_USING_STREAMING_JSON) {
                mStreamingProfile.setStream(new StreamingProfile.Stream(new JSONObject(publishAddr)));
            } else {
                mStreamingProfile.setPublishUrl(publishAddr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            dismissProgressDialog();
            showToast("无效的推流地址 !", Toast.LENGTH_SHORT);
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            dismissProgressDialog();
            showToast("无效的推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        mRTCStreamingManager.setStreamingProfile(mStreamingProfile);
        if (!mRTCStreamingManager.startStreaming()) {
            dismissProgressDialog();
            showToast(getString(R.string.failed_to_start_streaming), Toast.LENGTH_SHORT);
            return false;
        }
        dismissProgressDialog();
        showToast(getString(R.string.start_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        mIsPublishStreamStarted = true;
        /**
         * Because `startPublishStreaming` need a long time in some weak network
         * So we should check if the activity paused.
         */
        if (mIsActivityPaused) {
            stopPublishStreaming();
        }
        return true;
    }

    private boolean stopPublishStreaming() {
        if (!mIsPublishStreamStarted) {
            return true;
        }
        mRTCStreamingManager.stopStreaming();
        mIsPublishStreamStarted = false;
        showToast(getString(R.string.stop_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return false;
    }

    private StreamingStateChangedListener mStreamingStateChangedListener = new StreamingStateChangedListener() {
        @Override
        public void onStateChanged(final StreamingState state, Object o) {
            switch (state) {
                case PREPARING:
                    setStatusText(getString(R.string.preparing));
                    Log.d(TAG, "onStateChanged state:" + "preparing");
                    break;
                case READY:
                    mIsInReadyState = true;
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "ready");
                    break;
                case CONNECTING:
                    Log.d(TAG, "onStateChanged state:" + "connecting");
                    break;
                case STREAMING:
                    setStatusText(getString(R.string.streaming));
                    Log.d(TAG, "onStateChanged state:" + "streaming");
                    break;
                case SHUTDOWN:
                    mIsInReadyState = true;
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "shutdown");
                    break;
                case UNKNOWN:
                    Log.d(TAG, "onStateChanged state:" + "unknown");
                    break;
                case SENDING_BUFFER_EMPTY:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer empty");
                    break;
                case SENDING_BUFFER_FULL:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer full");
                    break;
                case OPEN_CAMERA_FAIL:
                    Log.d(TAG, "onStateChanged state:" + "open camera failed");
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    Log.d(TAG, "onStateChanged state:" + "audio recording failed");
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                case IOERROR:
                    /**
                     * Network-connection is unavailable when `startStreaming`.
                     * You can do reconnecting or just finish the streaming
                     */
                    Log.d(TAG, "onStateChanged state:" + "io error");
                    showToast(getString(R.string.io_error), Toast.LENGTH_SHORT);
                    sendReconnectMessage();
                    // stopPublishStreaming();
                    break;
                case DISCONNECTED:
                    /**
                     * Network-connection is broken after `startStreaming`.
                     * You can do reconnecting in `onRestartStreamingHandled`
                     */
                    Log.d(TAG, "onStateChanged state:" + "disconnected");
                    setStatusText(getString(R.string.disconnected));
                    // we will process this state in `onRestartStreamingHandled`
                    break;
            }
        }
    };

    private StreamingSessionListener mStreamingSessionListener = new StreamingSessionListener() {
        @Override
        public boolean onRecordAudioFailedHandled(int code) {
            return false;
        }

        /**
         * When the network-connection is broken, StreamingState#DISCONNECTED will notified first,
         * and then invoked this method if the environment of restart streaming is ready.
         *
         * @return true means you handled the event; otherwise, given up and then StreamingState#SHUTDOWN
         * will be notified.
         */
        @Override
        public boolean onRestartStreamingHandled(int code) {
            Log.d(TAG, "onRestartStreamingHandled, reconnect ...");
            return mRTCStreamingManager.startStreaming();

        }

        @Override
        public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
            return null;
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING || mIsActivityPaused || !mIsPublishStreamStarted) {
                return;
            }
            if (!StreamUtils.isNetworkAvailable(CapStreamingActivity.this)) {
                sendReconnectMessage();
                return;
            }
            Log.d(TAG, "do reconnecting ...");
            mRTCStreamingManager.startStreaming();
        }
    };

    private void sendReconnectMessage() {
        showToast("正在重连...", Toast.LENGTH_SHORT);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
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

    private RTCUserEventListener mRTCUserEventListener = new RTCUserEventListener() {
        @Override
        public void onUserJoinConference(String remoteUserId) {
            Log.d(TAG, "onUserJoinConference: " + remoteUserId);
        }

        @Override
        public void onUserLeaveConference(String remoteUserId) {
            Log.d(TAG, "onUserLeaveConference: " + remoteUserId);
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

    private View.OnClickListener mConferenceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConferenceCheckBox.isChecked()) {
                startConference();
            } else {
                stopConference();
            }
        }
    };

    private View.OnClickListener cbBeautyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (cbBeauty.isChecked()) {
                CameraStreamingSetting.FaceBeautySetting fbSetting = cameraStreamingSetting.getFaceBeautySetting();
                fbSetting.beautyLevel = 0.8f;
                fbSetting.whiten = 0.8f;
                fbSetting.redden = 0.6f;

                mRTCStreamingManager.updateFaceBeautySetting(fbSetting);
            } else {
                CameraStreamingSetting.FaceBeautySetting fbSetting = cameraStreamingSetting.getFaceBeautySetting();
                fbSetting.beautyLevel = 0.0f;
                fbSetting.whiten = 0.0f;
                fbSetting.redden = 0.0f;

                mRTCStreamingManager.updateFaceBeautySetting(fbSetting);
            }
        }
    };

    public void onClickStreaming(View v) {
        if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
            if (!mIsPublishStreamStarted) {
                startPublishStreaming();
            } else {
                stopPublishStreaming();
            }
        } else {
            if (!mIsConferenceStarted) {
                startConference();
            } else {
                stopConference();
            }
        }
    }

    private void setStatusText(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusTextView.setText(status);
            }
        });
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRole == StreamUtils.RTC_ROLE_ANCHOR) {
                    if (mIsPublishStreamStarted) {
                        mControlButton.setText(getString(R.string.stop_streaming));
                    } else {
                        mControlButton.setText(getString(R.string.start_streaming));
                    }
                } else {
                    if (mIsConferenceStarted) {
                        mControlButton.setText(getString(R.string.stop_conference));
                    } else {
                        mControlButton.setText(getString(R.string.start_conference));
                    }
                }
            }
        });
    }

    private void setConferenceBoxChecked(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConferenceCheckBox.setChecked(enabled);
            }
        });
    }

    private StreamStatusCallback mStreamStatusCallback = new StreamStatusCallback() {
        @Override
        public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String stat = "bitrate: " + streamStatus.totalAVBitrate / 1024 + " kbps"
                            + "\naudio: " + streamStatus.audioFps + " fps"
                            + "\nvideo: " + streamStatus.videoFps + " fps";
                    mStatTextView.setText(stat);
                }
            });
        }
    };

    protected void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        if (mIsActivityPaused) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(CapStreamingActivity.this, text, duration);
                mToast.show();
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

    private static boolean saveBitmapToSDCard(String filepath, Bitmap bitmap) {
        try {
            FileOutputStream fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    // PG Vars ============
    private final static String LOG_TAG = "SSSSS";
    //    private static SurfaceHolder m_pSurfaceHolder;
    private int m_iSurfaceWidth;
    private int m_iSurfaceHeight;
    //    private PGGLContextManager m_pGlContext;
    private Camera m_pCamera;

    //    private Camera.Size m_sCameraPreviewFrameSize;
    private Object m_pCameraLock = new Object();
    private SurfaceTexture m_pCameraTexture;
    private int m_iCameraTextureID;
    PGSkinPrettifyEngine m_pPGSkinPrettifyEngine;

    private boolean m_bIsFirstFrame;
    private Rect m_FaceRect = new Rect(0, 0, 0, 0);


    //value
    private float mfPinkValue = 0.6f;
    private float mfWhitenValue = 0.5f;
    private float mfReddenValue = 0.6f;
    private int mnSoftenValue = 70;
    private boolean mbClear = false;
    private int mnFilterValue = 100;

    public static final String SDK_KEY_NEW = "STGATA84usEsT9qo5s6q713+duRoDicxy2pwxiMrZRpqEKNztTSZv2tSF0uuvVfD1zvy/g7z47DmwIGM24xwCLW835Vw66mYU1xQkxR4QrrQ7dNWcpssHPnxZwAm2a5VLlIlnpelFqpNqFyP4fFSTMF+36Ur+rnAaFJJ6jevbBo=";

    long mAverConsume = 0;


    //滤镜列表
    private String[] mFilterName = {"深度美白", "艺术黑白", "清新丽人", "暖暖阳光", "香艳红唇",
            "日系甜美", "清凉", "唯美", "果冻", "淡雅", "清新", "温暖",
            "电影色FM2", "电影色FM7", "浅回忆", "薄荷绿", "电影（Lmo）", "Vista", "Portra",
            "Silk", "Amber", "Lens", "Polaroid"};
    private String[] mFilterType = {"Deep", "Skinbw", "Skinfresh", "Sunshine", "Sexylips", "Sweet",
            "Lightcool", "Grace", "Jelly", "Elegant", "Fresh", "Lightwarm", "FM2", "FM7", "Memory",
            "Mintgreen", "Movie", "Vista", "Portra", "Silk", "Amber", "Lens", "Polaroid"};

    private RecyclerView mListView;
    private MyRecycleAdapter mAdapter;
    private List<IRecycleCell> mListData;
    private String mCurFilterStrength;
    private boolean mbAlgorith = true;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seek_pink: {
                if (fromUser) {
                    mfPinkValue = progress / 100f;
                    m_pPGSkinPrettifyEngine.SetSkinColor(mfPinkValue, mfWhitenValue, mfReddenValue);
                    TextView tv_pink = (TextView) findViewById(R.id.pink_value);
                    tv_pink.setText(String.valueOf(mfPinkValue));
                }
            }
            break;
            case R.id.seek_whiten: {
                if (fromUser) {
                    mfWhitenValue = progress / 100f;
                    m_pPGSkinPrettifyEngine.SetSkinColor(mfPinkValue, mfWhitenValue, mfReddenValue);
                    TextView tv_whiten = (TextView) findViewById(R.id.whiten_value);
                    tv_whiten.setText(String.valueOf(mfWhitenValue));
                }
            }
            break;
            case R.id.seek_redden: {
                if (fromUser) {
                    mfReddenValue = progress / 100f;
                    m_pPGSkinPrettifyEngine.SetSkinColor(mfPinkValue, mfWhitenValue, mfReddenValue);
                    TextView tv_redden = (TextView) findViewById(R.id.redden_value);
                    tv_redden.setText(String.valueOf(mfReddenValue));
                }
            }
            break;
            case R.id.filter_redden: {
                if (fromUser) {
                    mnFilterValue = progress;
                    m_pPGSkinPrettifyEngine.SetColorFilterStrength(mnFilterValue);
                    TextView tv_blur = (TextView) findViewById(R.id.filter_value);
                    tv_blur.setText(String.valueOf(mnFilterValue));
                }
            }
            break;
            case R.id.seek_soften: {
                if (fromUser) {
                    mnSoftenValue = progress;
                    m_pPGSkinPrettifyEngine.SetSkinSoftenStrength(mnSoftenValue);
                    TextView tv_soften = (TextView) findViewById(R.id.soften_value);
                    tv_soften.setText(String.valueOf(mnSoftenValue));
                }
            }
            break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void writeFile(String fileName, byte[] b) throws IOException {
        try {
            File file = new File(fileName);

            FileOutputStream fos = new FileOutputStream(file);

            byte[] bytes = b;

            fos.write(bytes);

            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSurfaceCreated() {
        Log.e("SSSSS", "onSurfaceCreated");
//        m_pSurfaceHolder = mCameraPreviewFrameView.getHolder();
//        mainInit(m_pSurfaceHolder, true);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.e("hugo", "onSurfaceChanged width=="+width+";height==>"+height);
        m_bIsFirstFrame = true;
        m_iSurfaceWidth = width;
        m_iSurfaceHeight = height;
    }

    @Override
    public void onSurfaceDestroyed() {
        Log.e("SSSSS", "onSurfaceDestroyed");

    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        Log.e("SSSSSS", "onDrawFrame");
//        if (true)
//            return texId;
        time = System.currentTimeMillis();
        if (m_pPGSkinPrettifyEngine == null) return 0;

        if (m_bIsFirstFrame) {
            Log.e("hugo", "m_bIsFirstFrame");
            // 初始化引擎
            m_pPGSkinPrettifyEngine.InitialiseEngine(this, SDK_KEY_NEW, false);
            m_pPGSkinPrettifyEngine.SetSizeForAdjustInput(texWidth, texHeight);
            m_pPGSkinPrettifyEngine.SetOutputFormat(PGSkinPrettifyEngine.PG_PixelFormat.PG_Pixel_NV21);
            m_pPGSkinPrettifyEngine.SetOutputOrientation(PGSkinPrettifyEngine.PG_Orientation.PG_OrientationNormal);
            m_pPGSkinPrettifyEngine.SetSkinSoftenStrength(mnSoftenValue);
            m_pPGSkinPrettifyEngine.SetSkinColor(mfPinkValue, mfWhitenValue, mfReddenValue);
            m_pPGSkinPrettifyEngine.SetSkinSoftenAlgorithm(mbAlgorith ? PGSkinPrettifyEngine.PG_SoftenAlgorithm.PG_SoftenAlgorithmDenoise : PGSkinPrettifyEngine.PG_SoftenAlgorithm.PG_SoftenAlgorithmContrast);
            m_bIsFirstFrame = false;
        }
        Log.e("hugo", "onDrawFrame texId==>"+texId+";texWidth ==>"+texWidth+";texHeight ==>"+texHeight);
        m_pPGSkinPrettifyEngine.SetInputFrameByTexture(texId, texWidth, texHeight);
        m_pPGSkinPrettifyEngine.RunEngine();
        m_pPGSkinPrettifyEngine.GetOutputToScreen(texWidth, texHeight);

        time = System.currentTimeMillis();

        buffer = m_pPGSkinPrettifyEngine.SkinSoftenGetResult();
        Log.e("SSSSS", "onDrawFrame  COST TIME==" + (System.currentTimeMillis() - time));
        if (buffer != null) {
            lock.lock();
            buffer.clear();

            bytes2 = new byte[buffer.capacity()];

            buffer.get(bytes2, 0, bytes2.length);
            lock.unlock();
        }
        if (texId == m_pPGSkinPrettifyEngine.GetOutputTextureID())
            Log.e("SSSSS", "NO EFFECT");

        //Log.e("SSSSS", "onDrawFrame COST TIME==" + (System.currentTimeMillis() - time));
        return m_pPGSkinPrettifyEngine.GetOutputTextureID();
    }

    ByteBuffer buffer;
    byte[] bytes2;
    long time;
    Lock lock = new ReentrantLock();
    boolean isShoot = false;

    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height, int rotation, int fmt, long tsInNanoTime) {
        Log.e("SSSSS", "onPreviewFrame");
        Log.e("hugo", "onPreviewFrame: width==>"+width+";height==>"+height);
        time = System.currentTimeMillis();
        if (bytes2 != null) {
            lock.lock();
            System.arraycopy(bytes2, 0, bytes, 0, bytes.length);
            lock.unlock();
        }
        //Log.e("SSSSS", "onPreviewFrame COST TIME==" + (System.currentTimeMillis() - time));
        return false;
    }

    @Override
    public void onItemClick(String filtertype) {
        if (TextUtils.isEmpty(filtertype))
            return;
        if (m_pPGSkinPrettifyEngine == null)
            return;
        if (filtertype.equals(mCurFilterStrength)) {
            return;
        }
        mCurFilterStrength = filtertype;
        m_pPGSkinPrettifyEngine.SetColorFilterByName(filtertype);
        m_pPGSkinPrettifyEngine.SetColorFilterStrength(mnFilterValue);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public String getCurFilterType() {
        return mCurFilterStrength;
    }
}
