package com.codewithkael.remotecontrol.utils.webrt

import android.util.Log
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class RTCClientImpl(
    connection: PeerConnection,
    private val transferListener: TransferDataToServerCallback
) : RTCClient {

    companion object {
        private const val TAG = "RTC_LOG"
    }

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    override val peerConnection: PeerConnection = connection

    override fun offer() {
        Log.d(TAG, "Creating offer...")
        peerConnection.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        Log.d(TAG, "Local offer set successfully")
                    }
                }, desc)
                desc?.let {
                    Log.d(TAG, "Transferring offer to server...")
                    transferListener.onOfferGenerated(desc)
                }
            }
        }, mediaConstraint)
    }

    override fun answer() {
        Log.d(TAG, "Creating answer...")
        peerConnection.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        Log.d(TAG, "Local answer set successfully")
                        desc?.let { 
                            Log.d(TAG, "Transferring answer to server...")
                            transferListener.onAnswerGenerated(it) 
                        }
                    }
                }, desc)
            }
        }, mediaConstraint)
    }


    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        Log.d(TAG, "Setting remote description: ${sessionDescription.type}")
        peerConnection.setRemoteDescription(object : MySdpObserver() {
            override fun onSetSuccess() {
                super.onSetSuccess()
                Log.d(TAG, "Remote description set successfully")
            }
        }, sessionDescription)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        Log.d(TAG, "Adding remote ICE candidate: ${iceCandidate.sdpMid}")
        peerConnection.addIceCandidate(iceCandidate)
    }


    override fun onDestroy() {
        Log.d(TAG, "Closing PeerConnection")
        runCatching {
            peerConnection.close()
        }
    }


    override fun onLocalIceCandidateGenerated(iceCandidate: IceCandidate) {
        Log.d(TAG, "Local ICE candidate generated: ${iceCandidate.sdpMid}")
        peerConnection.addIceCandidate(iceCandidate)
        transferListener.onIceGenerated(iceCandidate)
    }

    interface TransferDataToServerCallback {
        fun onIceGenerated(iceCandidate: IceCandidate)
        fun onOfferGenerated(sessionDescription: SessionDescription)
        fun onAnswerGenerated(sessionDescription: SessionDescription)
    }
}
