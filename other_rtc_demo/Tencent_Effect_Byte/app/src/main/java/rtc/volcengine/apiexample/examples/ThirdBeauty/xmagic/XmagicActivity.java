package rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.data.VideoFrameInfo;
import com.ss.bytertc.engine.data.VideoOrientation;
import com.ss.bytertc.engine.handler.IRTCVideoEventHandler;
import com.ss.bytertc.engine.type.LocalVideoStreamError;
import com.ss.bytertc.engine.type.LocalVideoStreamState;
import com.tencent.demo.xmagic.CustomPropertyManager;
import com.tencent.demo.xmagic.TEBeautyAdapterSettingsView;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.effect.beautykit.view.panelview.TEPanelView;
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback;
import com.tencent.xmagic.XmagicApi;
import com.tencent.xmagic.XmagicConstant;

import java.lang.reflect.Type;
import java.util.List;

import rtc.volcengine.apiexample.BaseActivity;
import rtc.volcengine.apiexample.R;
import rtc.volcengine.apiexample.common.LicenseConstant;
import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api.ITEBeautyAdapter;
import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.impl.TEBeautyVolcAdapter;


public class XmagicActivity extends BaseActivity implements TEPanelViewCallback, TEBeautyAdapterSettingsView.CallBack {
    private static final String TAG = "XmagicActivity";


    private FrameLayout localViewContainer;


    private RTCVideo rtcVideo;

    private LinearLayout mPanelLayout;

    private TEBeautyVolcAdapter adapter = null;

    private TEBeautyAdapterSettingsView settingsView = null;

    private TEPanelView mTEPanelView = null;

    private String beautyLastParams = null;

    private final CustomPropertyManager customPropertyManager = new CustomPropertyManager();

    private TEBeautyKit mBeautyKit = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtils.setLogLevel(Log.VERBOSE);
        setContentView(R.layout.activity_xmagic);
        setTitle("腾讯美颜");
        initUI();
        // 创建引擎
        rtcVideo = RTCVideo.createRTCVideo(this, LicenseConstant.APP_ID, videoEventHandler, null, null);
        rtcVideo.setVideoOrientation(settingsView.getSettingViewMode().videoOrientation);

        XmagicConstant.DeviceLevel level = TEBeautyKit.getDeviceLevel(getApplicationContext());
        //判断设备等级，当低于终端机的时候，使用 NORMAL 模式
        if (level.getValue() < XmagicConstant.DeviceLevel.DEVICE_LEVEL_MIDDLE.getValue()) {
            this.adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.NORMAL, TEBeautyKit.getResPath());
        } else {
            this.adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.PRO, TEBeautyKit.getResPath());
        }
        adapter.notifyVideoOrientationChanged(settingsView.getSettingViewMode().videoOrientation);
        adapter.notifyScreenOrientationChanged(settingsView.getSettingViewMode().screenOrientation);
        // 设置本端渲染视图
        setLocalRenderView();
        // 开启音视频采集
        rtcVideo.startVideoCapture();
        enableBeauty();
    }


    private void initUI() {
        this.settingsView = new TEBeautyAdapterSettingsView(this);
        this.settingsView.setCallBack(this);
        localViewContainer = findViewById(R.id.local_view_container);
        this.mPanelLayout = findViewById(R.id.beauty_panel_layout);
        findViewById(R.id.test_view).setOnClickListener(v -> {
            TEBeautyAdapterSettingsView.showDialog(v.getContext(), settingsView);
        });
        mTEPanelView = new TEPanelView(this);
        mTEPanelView.showView(this);
        this.mPanelLayout.addView(mTEPanelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    }


    /**
     * 设置本地渲染视图，支持TextureView和SurfaceView
     */
    private void setLocalRenderView() {
        TextureView textureView = new TextureView(this);
        localViewContainer.removeAllViews();
        localViewContainer.addView(textureView);

        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = textureView;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置本地视频渲染视图
        rtcVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
    }


    /**
     * 引擎回调信息
     */
    IRTCVideoEventHandler videoEventHandler = new IRTCVideoEventHandler() {
        @Override
        public void onLocalVideoStateChanged(StreamIndex streamIndex, LocalVideoStreamState state, LocalVideoStreamError error) {
            super.onLocalVideoStateChanged(streamIndex, state, error);
            Log.i(TAG, "onLocalVideoStateChanged-streamIndex:" + streamIndex + " state:" + state);
        }

        @Override
        public void onLocalVideoSizeChanged(StreamIndex streamIndex, VideoFrameInfo frameInfo) {
            super.onLocalVideoSizeChanged(streamIndex, frameInfo);
            Log.i(TAG, "onLocalVideoSizeChanged-streamIndex:" + streamIndex + " frameInfo+" + frameInfo);
        }

        @Override
        public void onFirstLocalVideoFrameCaptured(StreamIndex streamIndex, VideoFrameInfo frameInfo) {
            super.onFirstLocalVideoFrameCaptured(streamIndex, frameInfo);
            Log.i(TAG, "onFirstLocalVideoFrameCaptured-streamIndex:" + streamIndex + " frameInfo:" + frameInfo);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcVideo != null) {
            rtcVideo.stopAudioCapture();
            rtcVideo.stopVideoCapture();
        }
        if (this.adapter != null) {
            this.adapter.unbind();
        }
        RTCVideo.destroyRTCVideo();
    }


    @Override
    public void onClickCustomSeg(TEUIProperty teuiProperty) {
        if (this.mBeautyKit == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.setData(teuiProperty, this.mBeautyKit, this.mTEPanelView);
        this.customPropertyManager.pickMedia(this, CustomPropertyManager.TE_CHOOSE_PHOTO_SEG_CUSTOM, CustomPropertyManager.PICK_CONTENT_ALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.mBeautyKit == null || this.customPropertyManager == null) {
            return;
        }
        this.customPropertyManager.onActivityResult(this, requestCode, resultCode, data);
    }


    @Override
    public void onCameraClick() {

    }

    @Override
    public void onUpdateEffected(List<TEUIProperty.TESDKParam> sdkParams) {

    }

    @Override
    public void onEffectStateChange(TEBeautyKit.EffectState effectState) {
        if (this.adapter != null) {
            ITEBeautyAdapter.AdapterEffectState adapterEffectState = (effectState == TEBeautyKit.EffectState.ENABLED) ? ITEBeautyAdapter.AdapterEffectState.ENABLED : ITEBeautyAdapter.AdapterEffectState.DISABLED;
            this.adapter.notifyEffectStateChanged(adapterEffectState);
        }
    }

    @Override
    public void onTitleClick(TEUIProperty uiProperty) {

    }


    @Override
    public void onCameraChange(boolean isFront) {
        rtcVideo.switchCamera(isFront ? CameraId.CAMERA_ID_FRONT : CameraId.CAMERA_ID_BACK);
        if (this.adapter != null) {
            adapter.notifyCameraChanged(isFront);
        }
    }

    @Override
    public void onBindBeauty(boolean isBindBeauty) {
        if (isBindBeauty) {
            this.enableBeauty();
        } else {
            this.adapter.unbind();
        }
    }

    private void enableBeauty() {
        this.adapter.bind(this, rtcVideo, new ITEBeautyAdapter.CallBack() {

            @Override
            public void onCreatedTEBeautyApi(XmagicApi xmagicApi) {
                mBeautyKit = new TEBeautyKit(xmagicApi);
                setLastParam(mBeautyKit);
                mTEPanelView.setupWithTEBeautyKit(mBeautyKit);

            }

            @Override
            public void onDestroyTEBeautyApi() {
                if (mBeautyKit != null) {
                    beautyLastParams = mBeautyKit.exportInUseSDKParam();
                }
                mBeautyKit = null;
            }
        });
    }

    private void setLastParam(TEBeautyKit beautyKit) {
        if (!TextUtils.isEmpty(beautyLastParams)) {
            Type type = (new TypeToken<List<TEUIProperty.TESDKParam>>() {
            }).getType();
            try {
                List<TEUIProperty.TESDKParam> paramList = (new Gson()).fromJson(beautyLastParams, type);
                beautyKit.setEffectList(paramList);
            } catch (Exception var4) {
                LogUtils.e(TAG, "JSON parsing failed, please check the json string");
                var4.printStackTrace();
            }
        }
    }

    @Override
    public void onVideoOrientationChange(VideoOrientation videoOrientation) {
        rtcVideo.setVideoOrientation(settingsView.getSettingViewMode().videoOrientation);
        if (adapter != null) {
            adapter.notifyVideoOrientationChanged(videoOrientation);
        }
    }

    @Override
    public void onScreenOrientationChange(int orientation) {
        this.setRequestedOrientation(orientation);
        if (adapter != null) {
            adapter.notifyScreenOrientationChanged(orientation);
        }
    }

    @Override
    public void onPanelVisibleChange(boolean isVisible) {
        if (mPanelLayout == null) {
            return;
        }
        mPanelLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
