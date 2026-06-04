package com.amazonaws.ivs.basicbroadcast.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import com.amazonaws.ivs.basicbroadcast.App
import com.amazonaws.ivs.basicbroadcast.common.AudioRecorder
import com.amazonaws.ivs.basicbroadcast.common.launchMain
import com.amazonaws.ivs.basicbroadcast.common.lazyViewModel
import com.amazonaws.ivs.basicbroadcast.common.showDialog
import com.amazonaws.ivs.basicbroadcast.data.LocalCacheProvider
import com.amazonaws.ivs.basicbroadcast.databinding.ActivityCustomBinding
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.amazonaws.ivs.basicbroadcast.effect.BeautyProcessor
import com.amazonaws.ivs.basicbroadcast.effect.CustomPropertyManager
import com.amazonaws.ivs.basicbroadcast.viewModel.CustomSourceViewModel
import com.amazonaws.ivs.broadcast.AudioDevice
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.ImagePreviewView
import com.tencent.effect.beautykit.TEBeautyKit
import com.tencent.effect.beautykit.config.TEUIConfig
import com.tencent.effect.beautykit.model.TEPanelDataModel
import com.tencent.effect.beautykit.model.TEUIProperty
import com.tencent.effect.beautykit.view.panelview.TEPanelView
import com.tencent.effect.beautykit.view.panelview.TEPanelViewCallback
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import okhttp3.internal.http2.Http2Reader
import javax.inject.Inject

private const val TAG = "AmazonIVS"

class CustomSourceActivity : PermissionActivity(), TEPanelViewCallback {

    @Inject
    lateinit var cacheProvider: LocalCacheProvider

    private val viewModel: CustomSourceViewModel by lazyViewModel(
        { this },
        { CustomSourceViewModel(application) })

    //    private var cameraManager: CameraManager? = null
    private var audioRecorder: AudioRecorder? = null


    private var imagePreviewView: ImagePreviewView? = null

    private lateinit var binding: ActivityCustomBinding

    private var trtcCloud: TRTCCloud? = null;
    private var surface: Surface? = null
    private var mTEPanelView: TEPanelView? = null;
    private val customPropertyManager = CustomPropertyManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        binding = ActivityCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.preview.observe(this) {
            Log.d(TAG, "Texture view changed: $it")
            binding.previewView.addView(it)
            imagePreviewView = it
        }

        viewModel.clearPreview.observe(this) { clear ->
            Log.d(TAG, "Texture view cleared")
            if (clear) binding.previewView.removeAllViews()
        }

        viewModel.indicatorColor.observe(this) { color ->
            Log.d(TAG, "Indicator color changed")
        }

        viewModel.errorHappened.observe(this) { error ->
            Log.d(TAG, "Error dialog is shown: ${error.first}, ${error.second}")
            showDialog(error.first, error.second)
        }

        viewModel.disconnectHappened.observe(this) {
            Log.d(TAG, "Disconnect happened")
            endSession()
        }

        initBackCallback()
        initUi()
    }

    private fun backPressed() {
        endSession()
        finish()
    }

    private fun initBackCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPressed()
            }
        })
    }

    private fun initBeautyView(beautyKit: TEBeautyKit?) {
        mTEPanelView = TEPanelView(this)
        mTEPanelView!!.setupWithTEBeautyKit(beautyKit)
        mTEPanelView!!.showView(this)
        binding.panelLayout.addView(
            mTEPanelView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }


    override fun onDestroy() {
        release()
        endSession()
        super.onDestroy()
    }



    private fun endSession() {
        Log.d(TAG, "Session ended")
        release()
        audioRecorder?.release()
        binding.previewView.removeAllViews()
        imagePreviewView = null
        viewModel.endSession()
        resetUi()
    }

    private fun initUi() {
        if (!arePermissionsGranted()) {
            askForPermissions { success ->
                if (success) {
                    createSessionAndAttachCustomSources()
                }
                startPreview()
            }
        } else {
            createSessionAndAttachCustomSources()
            startPreview()
        }
    }

    private fun resetUi() {

    }

    private fun createSessionAndAttachCustomSources() {
        createSession {
            startSession()
        }

    }

    private fun createSession(onReady: () -> Unit) {
        Log.d(TAG, "Session started")
        viewModel.createSession {
            onReady()
        }
    }

    private fun startSession() {
        try {
//            viewModel.session?.start(endpoint, key)
            attachCustomSources()
            viewModel.displayPreview()
        } catch (e: BroadcastException) {
            e.printStackTrace()
            launchMain {
                Log.d(TAG, "Error dialog is shown: ${e.code}, ${e.detail}")
                showDialog(e.code.toString(), e.detail)
            }
            endSession()
        }
    }

    private fun attachCustomSources() {
        Log.d(TAG, "Attaching custom sources")
        attachCustomCamera()

        // We're using a convenience method in AudioRecorder that was only introduced in Android 23.
        // Using the custom audio source is compatible back to Android 21, however. Instructions
        // on how to do so are provided in AudioRecorder.kt.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            attachCustomMicrophone()
        }
    }

    private fun attachCustomCamera() {
        viewModel.session?.createImageInputSource()?.let { surfaceSource ->
            if (this.trtcCloud == null) {
                surface = surfaceSource.inputSurface
                viewModel.beautyProcessor = BeautyProcessor(applicationContext) { beautyKit ->
                    launchMain {
                        initBeautyView(beautyKit)
                    }
                }
                this.trtcCloud = TRTCCloud.sharedInstance(applicationContext)
                this.trtcCloud?.setLocalVideoProcessListener(
                    TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D,
                    TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE,
                    viewModel.beautyProcessor
                )
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun attachCustomMicrophone() {
        // Most of the logic for appending audio data from the custom microphone to the
        // broadcast session is in the AudioRecorder class, created below.
        AudioRecorder(applicationContext).apply {
            audioRecorder = this

            val sampleRate = when (this.sampleRate) {
                8000 -> BroadcastConfiguration.AudioSampleRate.RATE_8000
                16000 -> BroadcastConfiguration.AudioSampleRate.RATE_16000
                22050 -> BroadcastConfiguration.AudioSampleRate.RATE_22050
                44100 -> BroadcastConfiguration.AudioSampleRate.RATE_44100
                48000 -> BroadcastConfiguration.AudioSampleRate.RATE_48000
                else -> BroadcastConfiguration.AudioSampleRate.RATE_44100
            }

            val format = when (this.bitDepth) {
                16 -> AudioDevice.Format.INT16
                32 -> AudioDevice.Format.FLOAT32
                else -> AudioDevice.Format.INT16
            }

            // Create a AudioDevice to receive the custom audio, using the configurations determined above.
            // In this case, configuration is hardcoded in the AudioRecorder class.
            viewModel.session?.createAudioInputSource(this.channels, sampleRate, format)
                ?.let { audioDevice ->
                    // Start streaming data from the microphone to the AudioDevice.
                    this.start(audioDevice)
                }
        }
    }


    fun startPreview() {
        Handler(Looper.getMainLooper()).post {
            this.trtcCloud?.startLocalPreview(true, TXCloudVideoView(applicationContext, surface))
        }
    }



    fun release() {
        this.trtcCloud?.stopLocalPreview()
        this.trtcCloud?.setLocalVideoProcessListener( TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D,
            TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE,null)
    }

    override fun onClickCustomSeg(uiProperty: TEUIProperty?) {
        val beautyKit = viewModel.beautyProcessor?.teBeautyKit ?: return
        customPropertyManager.setData(uiProperty, beautyKit, mTEPanelView)
        customPropertyManager.pickMedia(this, CustomPropertyManager.TE_CHOOSE_PHOTO_SEG_CUSTOM, CustomPropertyManager.PICK_CONTENT_ALL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val beautyKit = viewModel.beautyProcessor?.teBeautyKit ?: return
        customPropertyManager.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onCameraClick() {

    }

    override fun onUpdateEffected(sdkParams: MutableList<TEUIProperty.TESDKParam>?) {

    }

    override fun onEffectStateChange(effectState: TEBeautyKit.EffectState?) {

    }

    override fun onTitleClick(uiProperty: TEUIProperty?) {

    }
}
