package com.kang.administrator.zhisuntestapplication.camera.open

import android.hardware.Camera


@SuppressWarnings("deprecation")  // camera APIs
class OpenCamera(private val index: Int, val camera: Camera, val facing: CameraFacing, val orientation: Int) {

    override fun toString(): String {
        return "Camera #" + index + " : " + facing + ','.toString() + orientation
    }

}