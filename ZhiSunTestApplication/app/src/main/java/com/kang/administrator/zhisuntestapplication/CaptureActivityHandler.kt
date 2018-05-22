package com.kang.administrator.zhisuntestapplication

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.Browser
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.kang.administrator.zhisuntestapplication.camera.CameraManager
import kotlinx.android.synthetic.main.activity_capture.*
import java.util.AbstractCollection

class CaptureActivityHandler internal constructor(private val activity: CaptureActivity,
                                                  var decodeFormats: AbstractCollection<BarcodeFormat>?,
                                                  var baseHints: Map<DecodeHintType, Any>?,
                                                  var characterSet: String?,
                                                  private val cameraManager: CameraManager) : Handler() {
    private val decodeThread: DecodeThread
    private var state: State? = null

    enum class State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    init {
        Log.i("kang","cameraManager!!.openDriver(surfaceHolder)f")
        decodeThread = DecodeThread(activity, decodeFormats, baseHints, characterSet,
                ViewfinderResultPointCallback(activity.viewfinderView))
        Log.i("kang","cameraManager!!.openDriver(surfaceHolder)f")
        decodeThread.start()
        state = State.SUCCESS
        Log.i("kang","cameraManager!!.openDriver(surfaceHolder)lkusflasdfasdf")
        cameraManager.startPreview()
        restartPreviewAndDecode()
    }// Start ourselves capturing previews and decoding.

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.restart_preview -> restartPreviewAndDecode()
            R.id.decode_succeeded -> {
                state = State.SUCCESS
                val bundle = message.data
                var barcode: Bitmap? = null
                var scaleFactor = 1.0f
                if (bundle != null) {
                    val compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP)
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.size, null)
                        // Mutable copy:
                        barcode = barcode!!.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR)
                }
                activity.handleDecode(message.obj as Result, barcode, scaleFactor)
            }
            R.id.decode_failed -> {
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW
                cameraManager.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decode)
            }
            R.id.return_scan_result -> {
                activity.setResult(Activity.RESULT_OK, message.obj as Intent)
                activity.finish()
            }
            R.id.launch_product_query -> {
                val url = message.obj as String

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intents.FLAG_NEW_DOC)
                intent.data = Uri.parse(url)

                val resolveInfo = activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                var browserPackageName: String? = null
                if (resolveInfo != null && resolveInfo.activityInfo != null) {
                    browserPackageName = resolveInfo.activityInfo.packageName
                    Log.d(TAG, "Using browser in package " + browserPackageName!!)
                }

                // Needed for default Android browser / Chrome only apparently
                if (browserPackageName != null) {
                    when (browserPackageName) {
                        "com.android.browser", "com.android.chrome" -> {
                            intent.`package` = browserPackageName
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName)
                        }
                    }
                }

                try {
                    activity.startActivity(intent)
                } catch (ignored: ActivityNotFoundException) {
                    Log.w(TAG, "Can't find anything to handle VIEW of URI $url")
                }

            }
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        cameraManager.stopPreview()
        val quit = Message.obtain(decodeThread.getHandler(), R.id.quit)
        quit.sendToTarget()
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L)
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    private fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW
            cameraManager.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decode)
            activity.drawViewfinder()
        }
    }

    companion object {

        private val TAG = CaptureActivityHandler::class.java.simpleName
    }


}