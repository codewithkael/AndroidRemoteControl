package com.codewithkael.remotecontrol.utils.webrt

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class MySdpObserver : SdpObserver {
    companion object {
        private const val TAG = "RTC_LOG"
    }

    override fun onCreateSuccess(desc: SessionDescription?) {
        Log.d(TAG, "onCreateSuccess: ${desc?.type}")
    }

    override fun onSetSuccess() {
        Log.d(TAG, "onSetSuccess")
    }

    override fun onCreateFailure(p0: String?) {
        Log.e(TAG, "onCreateFailure: $p0")
    }

    override fun onSetFailure(p0: String?) {
        Log.e(TAG, "onSetFailure: $p0")
    }
}
