package com.tencent.effect.agora.demo.activity;

import static io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_FOCAL_LENGTH_TYPE.CAMERA_FOCAL_LENGTH_DEFAULT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.effect.adapter.agora.api.ITEBeautyAdapter;
import com.tencent.effect.adapter.agora.impl.TEBeautyAgoraAdapter;
import com.tencent.effect.agora.demo.beauty.LicenseConstant;
import com.tencent.effect.agora.demo.observer.AgoraTextureProvider;
import com.tencent.effect.agora.demo.utils.LogUtils;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import com.tencent.thirdbeauty.xmagic.CustomPropertyManager;
import com.tencent.thirdbeauty.xmagic.SettingViewMode;
import com.tencent.thirdbeauty.xmagic.TEBeautyAdapterSettingsView;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;
import com.vcube.tencent.effect.R;

import java.util.List;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;


/**
 * 使用 腾讯特效提供的 adapter 接入 腾讯特效
 */
public class AdapterActivity extends AppCompatActivity implements TEPanelViewCallback, TEBeautyAdapterSettingsView.CallBack {

    private static final String TAG = AdapterActivity.class.getName();


    private int localUid = 0;
    private int remoteUid = 0;

    private volatile boolean isJoined = false;
    private RtcEngine agoraEngine;

    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;

    private LinearLayout beautyPanelLayout = null;
    private TEPanelView panelView;

    private TEBeautyAgoraAdapter agoraAdapter = null;
    private AgoraTextureProvider agoraTextureProvider = new AgoraTextureProvider();
    private TEBeautyKit teBeautyKit = null;

    private final CustomPropertyManager customPropertyManager = new CustomPropertyManager();

    private TEBeautyAdapterSettingsView adapterSettingsView = null;


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            LogUtils.d(TAG, "onUserJoined  uid= " + uid);
            remoteUid = uid;
            runOnUiThread(() -> setupRemoteVideo(remoteUid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            isJoined = true;
            localUid = uid;
            LogUtils.d(TAG, "onJoinChannelSuccess  uid= " + uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            remoteUid = 0;
            LogUtils.d(TAG, "onUserOffline  uid= " + uid);
            runOnUiThread(() -> remoteVideoContainer.removeAllViews());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_adapter);
        this.initView();
        this.agoraAdapter = new TEBeautyAgoraAdapter(XmagicConstant.EffectMode.PRO, TEBeautyKit.getResPath());
        this.initAgoraEngine();
    }


    private void initView() {


        this.localVideoContainer = findViewById(R.id.local_video_view_container);
        this.remoteVideoContainer = findViewById(R.id.remote_video_view_container);
        this.beautyPanelLayout = findViewById(R.id.panel_layout);
        this.panelView = new TEPanelView(this);
        this.panelView.showView(this);
        this.beautyPanelLayout.addView(this.panelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        this.adapterSettingsView = new TEBeautyAdapterSettingsView(this);
        this.adapterSettingsView.setCallBack(this);

        findViewById(R.id.test_view).setOnClickListener(v -> {
            TEBeautyAdapterSettingsView.showDialog(v.getContext(), adapterSettingsView);
        });

        new Handler(Looper.getMainLooper()).postDelayed(this::joinChannel,100);
    }


    public void initAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = LicenseConstant.appId;
            config.mEventHandler = mRtcEventHandler;
            this.agoraEngine = RtcEngine.create(config);
            this.agoraEngine.enableVideo();

            CameraCapturerConfiguration.CaptureFormat captureFormat = new CameraCapturerConfiguration.CaptureFormat();
            captureFormat.width = 960;
            captureFormat.height = 540;
            captureFormat.fps = 20;
            CameraCapturerConfiguration configuration = new CameraCapturerConfiguration(getCurrentSettingViewMode().isFront ? CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT : CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR, CAMERA_FOCAL_LENGTH_DEFAULT, captureFormat);
            agoraEngine.setCameraCapturerConfiguration(configuration);
            this.agoraTextureProvider.setAgoraEngine(this.agoraEngine);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupRemoteVideo(int remoteUid) {
        LogUtils.e(TAG, "setupRemoteVideo  uid= " + remoteUid);
        SurfaceView remoteSurfaceView = new SurfaceView(getBaseContext());
        this.agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, remoteUid));
        this.remoteVideoContainer.addView(remoteSurfaceView);
    }


    @Override
    public void onClickCustomSeg(TEUIProperty teuiProperty) {
        if (this.teBeautyKit == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.setData(teuiProperty, this.teBeautyKit, this.panelView);
        this.customPropertyManager.pickMedia(this, CustomPropertyManager.TE_CHOOSE_PHOTO_SEG_CUSTOM, CustomPropertyManager.PICK_CONTENT_ALL);
    }

    @Override
    public void onCameraClick() {

    }

    @Override
    public void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams) {

    }


    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {
        if (this.agoraAdapter != null) {
            ITEBeautyAdapter.AdapterEffectState adapterEffectState = (effectState == TEBeautyKit.EffectState.ENABLED) ? ITEBeautyAdapter.AdapterEffectState.ENABLED : ITEBeautyAdapter.AdapterEffectState.DISABLED;
            this.agoraAdapter.notifyEffectStateChanged(adapterEffectState);
        }
    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

    }

    @Override
    public boolean onCameraChange(boolean isFront) {
        if (agoraEngine == null) {
            return false;
        }
        if (!isJoined) {
            return false;
        }
        agoraEngine.switchCamera();
        return true;
    }

    @Override
    public void onBindBeauty(boolean isBindBeauty) {
        if (isBindBeauty) {
            this.enableBeauty();
        } else {
            this.agoraAdapter.unbind();
        }
    }

    private void enableBeauty() {
        if (this.agoraAdapter == null) {
            return;
        }
        this.agoraAdapter.bind(getApplicationContext(), this.agoraTextureProvider, new ITEBeautyAdapter.CallBack() {
            @Override
            public void onCreatedTEBeautyApi(XmagicApi xmagicApi) {
                teBeautyKit = new TEBeautyKit(xmagicApi);
                panelView.setupWithTEBeautyKit(teBeautyKit);
                Log.e(TAG, "enableBeauty    onCreatedTEBeautyApi");
            }

            @Override
            public void onDestroyTEBeautyApi() {
                Log.e(TAG, "enableBeauty    onDestroyTEBeautyApi");
                teBeautyKit = null;
            }
        });
        this.agoraAdapter.notifyScreenOrientationChanged(getCurrentSettingViewMode().screenOrientation);
    }




    @Override
    public void onScreenOrientationChange(int orientation) {
        this.setRequestedOrientation(orientation);
        if (agoraAdapter != null) {
            agoraAdapter.notifyScreenOrientationChanged(orientation);
        }

    }

    @Override
    public void onEncoderMirrorChange(boolean isMirror) {
        if (agoraEngine == null) {
            return;
        }
        if (!isJoined) {
            return;
        }
        int result = this.agoraEngine.setRemoteRenderMode(remoteUid, Constants.RENDER_MODE_HIDDEN, isMirror ? Constants.VIDEO_MIRROR_MODE_ENABLED : Constants.VIDEO_MIRROR_MODE_DISABLED);
        LogUtils.e(TAG, "onEncoderMirrorChange  " + result);
    }

    @Override
    public void onPanelVisibleChange(boolean isVisible) {
        if (this.panelView != null) {
            this.panelView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    public void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        SurfaceView localSurfaceView = new SurfaceView(getBaseContext());
        localVideoContainer.addView(localSurfaceView);
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, localUid));

//        VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(
//                VideoEncoderConfiguration.VD_640x360,
//                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24,
//                VideoEncoderConfiguration.STANDARD_BITRATE,
//                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT);
//        configuration.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_AUTO;
//        agoraEngine.setVideoEncoderConfiguration(configuration);


        if (getCurrentSettingViewMode().isBindBeauty) {
            this.enableBeauty();
        }
        agoraEngine.startPreview();
        agoraEngine.joinChannel("", "sample", localUid, options);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.teBeautyKit == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.teBeautyKit != null) {
            this.teBeautyKit.onResume();
        }
        if (this.agoraEngine != null) {
            this.agoraEngine.resumeAudio();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.teBeautyKit != null) {
            this.teBeautyKit.onPause();
        }
        if (this.agoraEngine != null) {
            this.agoraEngine.pauseAudio();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        remoteVideoContainer.removeAllViews();
        localVideoContainer.removeAllViews();
        if (agoraEngine != null) {
            agoraEngine.registerVideoFrameObserver(null);
            agoraEngine.stopPreview();
            agoraEngine.leaveChannel();
            agoraEngine.disableVideo();
            agoraEngine = null;
        }
        this.agoraAdapter.unbind();
    }




    private SettingViewMode getCurrentSettingViewMode() {
        return this.adapterSettingsView.getSettingViewMode();
    }

}