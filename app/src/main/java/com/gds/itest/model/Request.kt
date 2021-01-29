package com.gds.itest.model

import androidx.annotation.RawRes
import com.gds.itest.R
import com.gds.itest.utils.Constants
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

import timber.log.Timber

data class Request(
        @SerializedName(Constants.FUNCTION_COMMAND_ID)
        val commandID: String,
        @SerializedName(Constants.FUNCTION_NAME)
        val name: String,
        @SerializedName(Constants.FUNCTION_KEY)
        val key: String,
        @SerializedName(Constants.FUNCTION_STATUS)
        var status: String?,
        @SerializedName(Constants.FUNCTION_RESULT)
        var result: String?,
        @SerializedName(Constants.FUNCTION_PROPERTY)
        var options: JsonObject?,
        @SerializedName(Constants.FUNCTION_NOTE)
        var notes: JsonObject?
) {

    fun setNote(note: Any?) {
        try {
            note?.let {
                Timber.d("set Note it $it")
                var a = JsonObject()
                when (it) {
                    is String -> {
                        a.addProperty("note", it)
                    }
                    is JsonObject -> {
                        a = it
                    }
                }
                notes = a
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setNoteJson(note: JsonObject?) {
        try {
            note?.let {
                Timber.d("setNoteJson $it")
                notes = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOpt(key: String) = options?.get(key)

    fun getTimeOut() = getOpt(Constants.OPT_TIME_OUT)?.asInt
            ?: Constants.FUNCTION_TIMEOUT_DEFAULT

    fun getTimeToRecord() = getOpt(Constants.OPT_TIME_RECORD)?.asInt
        ?: Constants.FUNCTION_TIME_RECORD_DEFAULT

    fun getFrequency() = getOpt(Constants.OPT_REQUENCY)?.asInt
            ?: Constants.FUNCTION_TIMEOUT_DEFAULT

    fun getVolume() = getOpt(Constants.OPT_VOLUME)?.asInt
            ?: Constants.FUNCTION_TIMEOUT_DEFAULT

    fun getColor()  = getOpt(Constants.OPT_COLOR)?.asString
            ?: Constants.FUNCTION_COLOR_DEFAULT

    fun getMic()  = getOpt(Constants.OPT_MIC)?.asString
        ?: Constants.FUNCTION_COLOR_DEFAULT

    fun getRadius()  = getOpt(Constants.OPT_RADIUS)?.asInt
            ?: Constants.FUNCTION_RADIUS_DEFAULT

    fun getXCell()  = getOpt(Constants.OPT_X_CELL)?.asInt
            ?: Constants.FUNCTION_X_CELL_DEFAULT

    fun getYCell()  = getOpt(Constants.OPT_Y_CELL)?.asInt
            ?: Constants.FUNCTION_Y_CELL_DEFAULT

    fun getCameraType()  = getOpt(Constants.OPT_CAMERA_TYPE)?.asString
            ?: Constants.FUNCTION_CAMERA_TYPE_BACK

    fun getCameraFileName()  = getOpt(Constants.OPT_CAMERA_FILE_NAME)?.asString?.plus(Constants.FUNCTION_CAMERA_FILE_SUBFIX)
            ?: Constants.FUNCTION_CAMERA_FILE_DEFAULT

    fun getNumberPhone()  = getOpt(Constants.OPT_NUMBER_PHONE)?.asString
            ?: Constants.FUNCTION_NUMBER_PHONE_DEFAULT

    fun createJsonResult(): String {
        return Gson().toJson(this)
    }

    @RawRes
    fun getImageResult(): Int? {
        return when (key) {
            Constants.TAG_KEYCOMPASS -> R.drawable.f_compass
            Constants.TAG_KEYMOTIONSENSOR -> R.drawable.f_motion
            Constants.TAG_KEYWIFI -> R.drawable.f_wifi
            Constants.TAG_KEYGPS -> R.drawable.f_gps
            Constants.TAG_KEYBLUETOOTH -> R.drawable.f_bluetooth
            Constants.TAG_KEYWIRELESS_CHARGING -> R.drawable.f_wireless_charge
            Constants.TAG_KEYRECORDBACK -> R.drawable.f_audioloopback
            Constants.TAG_KEYVIBRATION -> R.drawable.f_vibration
            Constants.TAG_KEYBATTERY -> R.drawable.f_battery
            Constants.TAG_KEYCHARGE -> R.drawable.f_charge
            Constants.TAG_KEYPROXIMITYSENSOR -> R.drawable.f_proximity
            Constants.TAG_KEYLIGHTSENSOR -> R.drawable.f_light
            Constants.TAG_KEYCAMFRONT -> R.drawable.f_camera
            Constants.TAG_KEYCAMERA -> R.drawable.f_camera
            Constants.TAG_KEYBARCODE -> R.drawable.f_camera
            Constants.TAG_KEYVIDEORECORD -> R.drawable.f_back_video
            Constants.TAG_KEYVIDEORECORDFRONT -> R.drawable.f_front_video
            Constants.TAG_KEYFLASH -> R.drawable.f_flash
            Constants.TAG_KEYSPEAKER -> R.drawable.f_loudspeaker
            Constants.TAG_KEYEARPHONE -> R.drawable.f_earphone
            Constants.TAG_KEYHALL_SENSOR -> R.drawable.f_hallic
            Constants.TAG_KEYHPHONE,
            Constants.TAG_KEYMICROHEADPHONE -> R.drawable.f_headphone
            Constants.TAG_KEYHJACK -> R.drawable.f_headphone_jack
            Constants.TAG_KEYBUTTON_HEADPHONE -> R.drawable.f_headphone_jack
            Constants.TAG_KEYMICROPHONE -> R.drawable.f_microphone
            Constants.TAG_KEYBACKLIGHT -> R.drawable.f_subkey_light
            Constants.TAG_KEYBUTTONKEYLIGHT -> R.drawable.f_subkey
            Constants.TAG_KEYNFC -> R.drawable.f_nfc
            Constants.TAG_KEYLED_NOTIFICATION -> R.drawable.f_led
            Constants.TAG_KEYSMSSENDING -> R.drawable.f_sms
            Constants.TAG_KEYCALL -> R.drawable.f_call
            Constants.TAG_KEYRADIO -> R.drawable.f_radio
            Constants.TAG_KEYBUTTON -> R.drawable.f_button
            Constants.TAG_KEYPRESSURE_SENSOR -> R.drawable.f_barometer
            Constants.TAG_KEYVIBRATION_MIC -> R.drawable.f_vibration
            else -> null
        }
    }

    companion object {
        @JvmStatic
        fun create(json: String): Request {
            return Gson().fromJson(json, Request::class.java)
        }
    }
}