package com.kang.administrator.zhisuntestapplication

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.kang.administrator.zhisuntestapplication.camera.CameraConfigurationManager
import com.kang.administrator.zhisuntestapplication.camera.CameraManager
import com.kang.administrator.zhisuntestapplication.camera.PreviewCallback
import kotlinx.android.synthetic.main.activity_chris.*
import java.io.IOException


class ChrisActivity : AppCompatActivity() ,SurfaceHolder.Callback{

    lateinit var mHolder: SurfaceHolder

    private var handler: CaptureActivityHandler? = null
    private var hasSurface: Boolean = false
    private lateinit var cameraManager: CameraManager
    private var state: CaptureActivityHandler.State? = null
    private var previewCallback:PreviewCallback
    private var cameraConfigurationManager:CameraConfigurationManager
    init {
        cameraConfigurationManager=CameraConfigurationManager(this)
        previewCallback=PreviewCallback(cameraConfigurationManager)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chris)
        Log.i("kang","sssssssssssss1ssssssssssssssssssssad")

    }

    @SuppressLint("MissingSuperCall")
    override fun onResume() {
        super.onPause()
        cameraManager = CameraManager(applicationContext);
        mHolder=surfaceView.holder
        //initCamera(mHolder)
        mHolder.addCallback(this)
        cameraManager.startPreview()
        viewfinderView.setCameraManager(cameraManager)
    }

    // SurfaceHolder.Callback必须实现的方法
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (holder == null) {
        }
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }

    }

    // SurfaceHolder.Callback必须实现的方法
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    // SurfaceHolder.Callback必须实现的方法
    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        if (surfaceHolder == null) {
            throw IllegalStateException("No SurfaceHolder provided")
        }
        if (cameraManager!!.isOpen) {
            return
        }
        try {
            cameraManager!!.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            cameraManager.startPreview()
            cameraManager.addcalllpreview()
        } catch (ioe: IOException) {
        } catch (e: RuntimeException) {
        }

    }

    private fun restartPreviewAndDecode() {
        if (state == CaptureActivityHandler.State.SUCCESS) {
            state = CaptureActivityHandler.State.PREVIEW
        }
    }


}
