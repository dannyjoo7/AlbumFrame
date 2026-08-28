package kr.joolabs.albumframe.application

interface CameraAccess {
    fun isAvailable(): Boolean

    fun isGranted(): Boolean

    fun requestPermission(): String
}
