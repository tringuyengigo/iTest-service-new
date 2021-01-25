package com.gds.itest.utils

import android.os.Environment

object Constants {

    const val TAG_KEYGROUP = "GROUP"
    const val TAG_KEYCOMPASS = "F1_COMPASS"
    const val TAG_KEYWIFI = "F2_WIFI"
    const val TAG_KEYGPS = "F3_GPS"
    const val TAG_KEYCALL = "F4_CALL"
    const val TAG_KEYRECORDBACK = "F5_RECORDBACK"
    const val TAG_KEYCAMFRONT = "F6_CAMFRONT"
    const val TAG_KEYCAMREAR = "F7_CAMREAR"
    const val TAG_KEYVIDEORECORD = "F8_VIDEORECORD"
    const val TAG_KEYPROXIMITYSENSOR = "F9_PROXIMITYSENSOR"
    const val TAG_KEYDIGITIZER = "F10_DIGITIZER"
    const val TAG_KEYBUTTON = "F11_BUTTON"
    const val TAG_KEYMULTITOUCH = "F12_MULTITOUCH"
    const val TAG_KEYMOTIONSENSOR = "F13_MOTIONSENSOR"
    const val TAG_KEY3DTOUCH = "F14_3DTOUCH"
    const val TAG_KEYBUTTON_HEADPHONE = "F15_BUTTON_HEADPHONE"
    const val TAG_KEYLCD = "F16_LCD"
    const val TAG_KEYSPEAKER = "F17_SPEAKER"
    const val TAG_KEYHPHONE = "F18_HPHONE"
    const val TAG_KEYMICROPHONE = "F19_MICROPHONE"
    const val TAG_KEYEARPHONE = "F20_EARPHONE"
    const val TAG_KEYVIBRATION = "F21_VIBRATION"
    const val TAG_KEYSELFIE = "F22_SELFIE"
    const val TAG_KEYTOUCHID = "F23_TOUCHID"
    const val TAG_KEYCHARGE = "F24_CHARGE"
    const val TAG_KEYBATTERY = "F25_BATTERY"
    const val TAG_KEYBLUETOOTH = "F26_BLUETOOTH"
    const val TAG_KEYSIM_DETECTION = "F27_SIM_DETECTION"
    const val TAG_KEYCAMERA = "F28_CAMERA"
    const val TAG_KEYFLASH = "F29_FLASH"
    const val TAG_KEYCALLMANUAL = "F30_CALLMANUAL"
    const val TAG_KEYRADIO = "F31_RADIO"
    const val TAG_KEYDIMMING = "F32_DIMMING"
    const val TAG_KEYLIGHTSENSOR = "F33_LIGHTSENSOR"
    const val TAG_KEYNFC = "F34_NFC"
    const val TAG_KEYPOWERCHECK = "F35_POWERCHECK"
    const val TAG_KEYSDCARDDETECTION = "F36_SDCARDDETECTION"
    const val TAG_KEYBUTTONKEYLIGHT = "F37_BUTTONKEYLIGHT"
    const val TAG_KEYBACKLIGHT = "F38_BACKLIGHT"
    const val TAG_KEYVIDEORECORDFRONT = "F39_VIDEORECORDFRONT"
    const val TAG_KEYSMSSENDING = "F40_SMSSENDING"
    const val TAG_KEYWIRELESS_CHARGING = "F41_WIRELESS_CHARGING"
    const val TAG_KEYPRESSURE_SENSOR = "F42_PRESSURE_SENSOR"
    const val TAG_KEYHALL_SENSOR = "F43_HALL_SENSOR"
    const val TAG_KEYLED_NOTIFICATION = "F44_LED_NOTIFICATION"
    const val TAG_KEYTOUCHSCREEN = "F45_TOUCHSCREEN"
    const val TAG_KEYBTGREEN = "F46_BTGREEN"
    const val TAG_KEYCAPTURE = "F47_CAPTURE"
    const val TAG_KEYCAPTUREREAR = "F48_CAPTUREREAR"
    const val TAG_KEYCOSGRADE = "F49_COSGRADE"
    const val TAG_KEYCOSFACE = "F50_COSFACE"
    const val TAG_KEYENGRAVING = "F51_ENGRAVING"
    const val TAG_KEYCOSMETIC = "F52_COSMETIC"
    const val TAG_KEYBROKEN = "F53_BROKEN"
    const val TAG_KEYBARCODE = "F54_BARCODE"
    const val TAG_KEYHJACK = "F55_HJACK"
    const val TAG_KEYWFSTREAM = "F56_WFSTREAM"
    const val TAG_KEY3G4G = "F57_3G/4G"
    const val TAG_KEYBTFUNC = "F58_BTFUNC"
    const val TAG_KEYMICROHEADPHONE = "F59_MICROHEADPHONE"
    const val TAG_KEYSIRI = "F60_SIRI"
    const val TAG_KEYCHECK_HP_HJ = "F61_CHECK_HP_HJ"
    const val TAG_KEYGAZE_CG = "F62_GAZE_CG"
    const val TAG_KEYGAZE_CMT = "F63_GAZE_CMT"
    const val TAG_KEYGAZE_ORT = "F64_GAZE_ORT"
    const val TAG_KEYCHECK_HEADPHONE = "F65_CHECK_HEADPHONE"
    const val TAG_KEYHEADPHONEAUTO = "F66_HEADPHONEAUTO"
    const val TAG_KEYDATETIME = "F67_DATETIME"
    const val TAG_KEYJAILBREAK = "F68_JAILBREAK"
    const val TAG_KEYCHECKOPENWF = "F69_CHECKOPENWF"
    const val TAG_KEYACCELEROMETER = "F70_ACCELEROMETER"
    const val TAG_KEYGYROSCOPE = "F71_GYROSCOPE"
    const val TAG_KEYMAGNETOMETER = "F72_MAGNETOMETER"
    const val TAG_KEYBTHOME = "F73_BT_HOME"
    const val TAG_KEYBTPOWER = "F74_BT_POWER"
    const val TAG_KEYBTMUTE = "F75_BT_MUTE"
    const val TAG_KEYBTVOLUMEDOWN = "F76_BT_VOLUMEDOWN"
    const val TAG_KEYBTVOLUMEUP = "F77_BT_VOLUMEUP"
    const val TAG_KEYBARTEST = "F78_BARTEST"
    const val TAG_KEYCALIBZ = "F79_CALIBZ"
    const val TAG_KEYBROKENLCD = "F80_BROKEN_LCD"
    const val TAG_KEYDIMMINGSPEAKER = "F82_DIMMING_SPEAKER"
    const val TAG_KEYEARPHONEPROXIMITY = "F83_EARPHONE_PROXIMITY"
    const val TAG_KEYGHOSTSCREEN = "F84_GHOSTSCREEN"
    const val TAG_KEYSLIGHTBENT = "F87_SCREENCURVER"
    const val TAG_KEYSOTG = "F88_OTG"
    const val TAG_KEYSETHEADPHONE = "F89_SETHEADPHONE"
    const val TAG_KEYSPEN = "F97_S_PEN"
    const val TAG_KEYHRM = "F98_HRM_SENSOR"
    const val TAG_KEYQUICKCHARGE = "F101_QUICK_CHARGE"
    const val TAG_KEYVIBRATION_MIC = "F103_VIBRATION_MIC"


    /**
     *=============    OPTION     ===============
     */

    const val FUNCTION_NAME = "function_name"
    const val FUNCTION_KEY = "function_key"
    const val FUNCTION_STATUS = "status"
    const val FUNCTION_RESULT = "result"
    const val FUNCTION_PROPERTY = "property"
    const val FUNCTION_NOTE = "note"


    const val STATUS_STARTED = "start"
    const val STATUS_FINISHED = "finished"

    const val OPT_TIME_OUT = "timeout"
    const val OPT_VOLUME = "volume"
    const val OPT_REQUENCY = "frequency"
    const val OPT_COLOR = "color"
    const val OPT_RADIUS = "radius"
    const val OPT_X_CELL = "row"
    const val OPT_Y_CELL = "col"
    const val OPT_CAMERA_FILE_NAME = "nameimg"
    const val OPT_CAMERA_TYPE = "camera"
    const val OPT_NUMBER_PHONE = "callnum"



    const val OPT_WIFI_INFO = "wifi_info"
    const val OPT_WIFI_INFO_LIST = "wifi_list"

    const val OPT_BLUETOOTH_INFO = "bluetooth_info"
    const val OPT_BLUETOOTH_INFO_LIST = "bluetooth_list"

    const val OPT_GPS_LAT = "latitude"
    const val OPT_GPS_LONG = "longitude"
    const val OPT_GPS_DISTANCE = "distance"


    const val NOTE_DATA = "data"
    const val NOTE_MAX_TOUCH = "max_touch"
    /**
     *=============    WORKER     ===============
     */
    const val WORKER_DATA_REQUEST = "data_request"
    const val WORKER_DATA_RESULT = "data_result"
    const val WORKER_DATA_NOTE = "data_note"
    const val WORKER_DATA_OPTION = "data_option"
    const val WORKER_UNIQUE_READ_REQUEST = "UNIQUE_REQUEST"
    const val WORKER_FUNCTION_PROCESS = "FUNCTION_PROCESS"
    const val TAG_WORKER_UNIQUE = "WORKER_UNIQUE"

    private val FILE_APP_PATH = "${Environment.getExternalStorageDirectory().path}/Itest/"
    val FILE_REQUEST_PATH = "${FILE_APP_PATH}request.txt"
    val FILE_REQUEST_AUTOMATION_PATH = "${FILE_APP_PATH}request_auto_test.txt"
    val FILE_RESULT_PATH = "${Environment.getExternalStorageDirectory().path}/Itest/"
    val FILE_DEBUG_PATH = Environment.getExternalStorageDirectory().path.plus("/debug.txt")

    const val ERROR_CODE_TIME_OUT = "Timeout"
    const val ERROR_CODE_HJACT_UNPLUGED = "Headphone jack is not unplugged"
    const val ERROR_CODE_HJACT_PLUGED = "Headphone jack is not plugged"
    const val ERROR_CODE_CAN_NOT_INIT = "Cannot init audio manager"

    const val ERROR_CODE_WIFI_CAN_NOT_CONNECT = "wifi cannot connect"
    const val ERROR_CODE_WIFI_CAN_NOT_PING_TO_NET = "wifi cannot access to internet"

    const val ERROR_CODE_BLUETOOTH_CAN_NOT_ACCESS = "Cannot connect adapter"

    const val ERROR_CODE_AUDIO_RECORD_CAN_NOT_CREATE_FILE = "File audio record can not create"
    const val ERROR_CODE_INIT_VIBRATION = "Cannot Init Vibration"
    const val RESULT_CODE_DETECT_BY_HW = "detected by software"


    const val PASSED = "passed"
    const val FAILED = "failed"

    const val FUNCTION_NOT_TEST = -1
    const val FUNCTION_PASSED = 0
    const val FUNCTION_FAILED = 1
    const val FUNCTION_FAILED_TIMEOUT = -9
    const val FUNCTION_TIMEOUT_DEFAULT = 10
    const val FUNCTION_FREQUENCY_DEFAULT = -1
    const val FUNCTION_VOLUME_DEFAULT = 8
    const val FUNCTION_DURATION_DEFAULT = 8_000L
    const val FUNCTION_COLOR_DEFAULT = "red"
    const val FUNCTION_RADIUS_DEFAULT = 60
    const val FUNCTION_MIN_IMAGE_SIZE = 50_000

    const val FUNCTION_X_CELL_DEFAULT = 2
    const val FUNCTION_Y_CELL_DEFAULT = 5
    const val FUNCTION_CAMERA_FILE_SUBFIX = ".jpg"
    const val FUNCTION_CAMERA_FILE_DEFAULT = "default".plus(FUNCTION_CAMERA_FILE_SUBFIX)
    const val FUNCTION_CAMERA_TYPE_BACK = "back"
    const val FUNCTION_CAMERA_TYPE_FRONT = "front"
    const val ACTIVITY_REQUEST_CODE_GPS_TURNON = 100
    const val FUNCTION_NUMBER_PHONE_DEFAULT = "109"


}