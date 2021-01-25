package com.gds.itest.interactor

import androidx.annotation.ColorInt
import com.gds.itest.model.Request

class EventAutoFunction(val datas: Array<Request>)
/**
 * OR local request
 */
class EventInitWifiBackground(val data : Request)
class EventInitBluetoothBackground(val data : Request)
class EventInitGPSBackground(val data : Request)


class EventDimmingScreen(val step: Int)

class EventColorScreen(@ColorInt val color: Int?)

/**
 * @param info.first (Int):
 *          Not Test -> add view
 *          FAILED,PASSED -> remove view and make result
 * @param info.second: radius touch size
 */
class EventDigitizeMultitouchScreen(val info: Pair<Int, Int?>)

class EventTouchScreen(val info: Pair<Int, Int?>)

class EventGhostTouchScreen(val info: Pair<Int, Int?>)

class EventTouchCellsScreen(val info: Pair<Int, Pair<Int,Int>?>)

class EventSpen(val info: Pair<Int, Pair<Int,Int>?>)

/**
 * @param info.second true: is check camera front, otherwise is back
 */
class EventCameraSimple(val info: Pair<Int,Boolean>)

class EventScanBarcode(val info: Pair<Int,Boolean>)

class EventCaptureVideo(val info: Request)

class EventCalibrate(val info: Request)