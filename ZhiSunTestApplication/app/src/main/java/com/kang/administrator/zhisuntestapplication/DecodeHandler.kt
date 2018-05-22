package com.kang.administrator.zhisuntestapplication

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream

internal class DecodeHandler(private val activity: CaptureActivity, hints: Map<DecodeHintType, Any>) : Handler() {
    private val multiFormatReader: MultiFormatReader
    private var running = true

    init {
        multiFormatReader = MultiFormatReader()
        multiFormatReader.setHints(hints)
    }

    override fun handleMessage(message: Message?) {
        if (message == null || !running) {
            return
        }
        when (message.what) {
            R.id.decode -> decode(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.quit -> {
                running = false
                Looper.myLooper()!!.quit()
            }
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        val start = System.currentTimeMillis()
        var rawResult: Result? = null
        val source = activity.cameraManager!!.buildLuminanceSource(data, width, height)
        Log.i("kang","width====${width} ; height====${height}")

        if (source != null) {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                Log.i("kang","bitmap = BinaryBitmap(HybridBinarizer(source))")
                rawResult = multiFormatReader.decodeWithState(bitmap)
            } catch (re: ReaderException) {
                // continue
            } finally {
                multiFormatReader.reset()
            }
        }

        val handler = activity.handler
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.currentTimeMillis()
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, rawResult)
                val bundle = Bundle()
                bundleThumbnail(source!!, bundle)
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            Log.i("kang","rawResult failed")
            if (handler != null) {
                val message = Message.obtain(handler, R.id.decode_failed)
                message.sendToTarget()
            }
        }
    }

    companion object {

        private val TAG = DecodeHandler::class.java.simpleName

        private fun bundleThumbnail(source: PlanarYUVLuminanceSource, bundle: Bundle) {
            val pixels = source.renderThumbnail()
            val width = source.thumbnailWidth
            val height = source.thumbnailHeight
            val bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
            bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray())
            bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, width.toFloat() / source.width)
        }
    }

}