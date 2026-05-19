package com.codewithkael.remotecontrol.utils.webrt

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import com.codewithkael.remotecontrol.utils.MyApplication
import com.codewithkael.remotecontrol.utils.webrt.IceServers.Companion.getIceServers
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCFactory @Inject constructor(
    private val application: Application
) {

    // ===== WebRTC core =====
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }

    private var videoCapture: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(true) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    private val streamId = "${MyApplication.UserID}_stream"
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null


    private val iceServer = getIceServers()

    init {
        initPeerConnectionFactory(application)
    }

    // Public API
    fun prepareScreenSharing(intentData: Intent, view: SurfaceViewRenderer) {
        initSurfaceView(view)
        startScreenCapture(intentData, view)
    }

    fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }

    fun createRTCClient(
        observer: PeerConnection.Observer, listener: RTCClientImpl.TransferDataToServerCallback
    ): RTCClient? {
        val connection = peerConnectionFactory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServer), observer
        )
        localVideoTrack?.let { connection?.addTrack(it, listOf(streamId)) }
        localAudioTrack?.let { connection?.addTrack(it, listOf(streamId)) }
        return connection?.let { RTCClientImpl(it, listener) }
    }

    fun onDestroy() {
        runCatching { videoCapture?.stopCapture() }
        runCatching { videoCapture?.dispose() }
        videoCapture = null

        runCatching { surfaceTextureHelper?.dispose() }
        surfaceTextureHelper = null

        localAudioTrack?.let {
            it.setEnabled(false)
            it.dispose()
        }
        localAudioTrack = null

        localVideoTrack?.dispose()
        localVideoTrack = null
    }

    private fun startScreenCapture(intentData: Intent, surface: SurfaceViewRenderer) {
        if (localVideoTrack == null) {
            surfaceTextureHelper =
                SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)

            videoCapture = ScreenCapturerAndroid(intentData, object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                }
            })

            videoCapture?.initialize(
                surfaceTextureHelper, surface.context, localVideoSource.capturerObserver
            )

            videoCapture?.startCapture(720, 1280, 15)

            localVideoTrack =
                peerConnectionFactory.createVideoTrack("${streamId}_video", localVideoSource)
        }
        localVideoTrack?.addSink(surface)

        if (localAudioTrack == null) {
            localAudioTrack =
                peerConnectionFactory.createAudioTrack("${streamId}_audio", localAudioSource)
        }
    }


    // PeerConnectionFactory
    private fun initPeerConnectionFactory(application: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .setOptions(
                PeerConnectionFactory.Options().apply {
                    disableEncryption = false
                    disableNetworkMonitor = false
                }).createPeerConnectionFactory()
    }
}
