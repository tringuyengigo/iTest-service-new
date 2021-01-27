package com.gds.itest.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.gds.itest.App

object AudioControl {
    private var currentVolume = 9

    private val audioManager: AudioManager by lazy {
        App.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @JvmStatic fun setStreamVolume(streamType: Int, volume: Int, flat: Int = 0) {
        currentVolume = volume
        audioManager.setStreamVolume(streamType, volume, flat)
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE,0)
    }

    @JvmStatic fun getStreamVolume(streamType: Int) : Int =
        audioManager.getStreamVolume(streamType)

    @JvmStatic fun setMode(mode: Int) {
        audioManager.mode = mode
    }

    @JvmStatic fun setSpeakerphoneOn(isSpeaker: Boolean) {
        audioManager.isSpeakerphoneOn = isSpeaker
    }

    @JvmStatic  fun checkHeadPhonePlugged(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val list = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            if (list == null || list.isEmpty()) return false
            else {
                list.map {
                    if (it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        return true
                    }
                }
                return false
            }
        } else {
            return audioManager.isWiredHeadsetOn
        }
    }

    @JvmStatic  fun getManager() = audioManager
}