package com.tencent.effect.agora.demo.activity;

import static io.agora.rtc2.Constants.VIDEO_MIRROR_MODE_AUTO;
import static io.agora.rtc2.Constants.VIDEO_MIRROR_MODE_DISABLED;
import static io.agora.rtc2.Constants.VIDEO_MIRROR_MODE_ENABLED;
import static io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_FOCAL_LENGTH_TYPE.CAMERA_FOCAL_LENGTH_DEFAULT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tencent.effect.agora.demo.beauty.TencentBeautyProcessor;
import com.tencent.effect.agora.demo.beauty.LicenseConstant;
import com.tencent.effect.agora.demo.observer.AgoraVideoFrameObserver;
import com.tencent.effect.agora.demo.utils.LogUtils;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import com.tencent.thirdbeauty.xmagic.CustomPropertyManager;
import com.vcube.tencent.effect.R;

import java.util.List;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;


public class MainActivity extends AppCompatActivity implements
        AgoraVideoFrameObserver.LocalRenderMirrorChangeListener, TEPanelViewCallback {

    private static final String TAG = MainActivity.class.getName();


    private RtcEngine agoraEngine;

    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;

    private TencentBeautyProcessor beautyProcessor = null;
    private TEPanelView panelView;
    private AgoraVideoFrameObserver agoraFrameObserver = null;

    private final CustomPropertyManager customPropertyManager = new CustomPropertyManager();

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            LogUtils.d(TAG, "onUserJoined " + uid);
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            LogUtils.d(TAG, "onJoinChannelSuccess " + uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            LogUtils.d(TAG, "onUserOffline " + uid);
            runOnUiThread(() -> remoteVideoContainer.removeAllViews());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.initView();
        this.beautyProcessor = new TencentBeautyProcessor(this);
        this.beautyProcessor.setBeautyPanel(panelView);
        this.initAgoraEngine();
    }


    private void initView() {


        this.localVideoContainer = findViewById(R.id.local_video_view_container);
        this.remoteVideoContainer = findViewById(R.id.remote_video_view_container);
        LinearLayout beautyPanelLayout = findViewById(R.id.panel_layout);
        this.panelView = new TEPanelView(this);
        this.panelView.showView(this);
        beautyPanelLayout.addView(this.panelView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));


        ((Switch) this.findViewById(R.id.switchCameraButton)).setOnCheckedChangeListener((compoundButton, b) -> {
            if (agoraEngine == null) {
                compoundButton.setChecked(false);
                return;
            }
            agoraEngine.switchCamera();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> joinChannel(), 100);
    }


    public void initAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = LicenseConstant.appId;
            config.mEventHandler = mRtcEventHandler;
            this.agoraEngine = RtcEngine.create(config);
            this.agoraEngine.enableVideo();

            this.agoraFrameObserver = new AgoraVideoFrameObserver(this.beautyProcessor);
            this.agoraFrameObserver.setRenderMirrorChangeListener(this);
            this.agoraEngine.registerVideoFrameObserver(this.agoraFrameObserver);

            CameraCapturerConfiguration.CaptureFormat captureFormat =
                    new CameraCapturerConfiguration.CaptureFormat();
//            captureFormat.width = 1280;
//            captureFormat.height = 720;
//            captureFormat.width = 960;
//            captureFormat.height = 540;
//            captureFormat.width = 852;
//            captureFormat.height = 480;
//            captureFormat.width = 640;
//            captureFormat.height = 360;

            captureFormat.fps = 20;
            CameraCapturerConfiguration configuration =
                    new CameraCapturerConfiguration(CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT,
                            CAMERA_FOCAL_LENGTH_DEFAULT, captureFormat);
//            this.agoraEngine.setLocalRenderMode(Constants.RENDER_MODE_HIDDEN,VIDEO_MIRROR_MODE_DISABLED);
            agoraEngine.setCameraCapturerConfiguration(configuration);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupRemoteVideo(int uid) {
        SurfaceView remoteSurfaceView = new SurfaceView(getBaseContext());
        VideoCanvas videoCanvas = new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid);
        videoCanvas.mirrorMode = VIDEO_MIRROR_MODE_ENABLED;   // 拉流端渲染设置镜像，
        agoraEngine.setupRemoteVideo(videoCanvas);
        remoteVideoContainer.addView(remoteSurfaceView);
    }


    @Override
    public void onLocalRenderMirrorChange(boolean localRenderMirror) {
        this.agoraEngine.setLocalRenderMode(Constants.RENDER_MODE_HIDDEN,
                localRenderMirror ? VIDEO_MIRROR_MODE_ENABLED : Constants.VIDEO_MIRROR_MODE_DISABLED);
    }

    @Override
    public void onClickCustomSeg(TEUIProperty teuiProperty) {
        if (this.beautyProcessor.getBeautyKit() == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.setData(teuiProperty, this.beautyProcessor.getBeautyKit(), this.panelView);
        this.customPropertyManager.pickMedia(this, CustomPropertyManager.TE_CHOOSE_PHOTO_SEG_CUSTOM,
                CustomPropertyManager.PICK_CONTENT_ALL);
    }

    @Override
    public void onCameraClick() {

    }

    @Override
    public void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams) {

    }


    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {

    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

    }

    public void joinChannel() {
        if (agoraEngine == null) {
            return;
        }
        int localUid = 0;
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        SurfaceView localSurfaceView = new SurfaceView(getBaseContext());
        localVideoContainer.addView(localSurfaceView);
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, localUid));
        agoraEngine.startPreview();
        agoraEngine.joinChannel("", "sample", localUid, options);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.beautyProcessor.getBeautyKit() == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.beautyProcessor.getBeautyKit() != null) {
            this.beautyProcessor.getBeautyKit().onResume();
        }
        if (this.agoraEngine != null) {
            this.agoraEngine.resumeAudio();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.beautyProcessor.getBeautyKit() != null) {
            this.beautyProcessor.getBeautyKit().onPause();
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
        new Thread(() -> agoraFrameObserver.release()).start();
    }


}