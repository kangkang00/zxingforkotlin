package com.kang.administrator.zhisuntestapplication

import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPointCallback
import java.util.*
import java.util.concurrent.CountDownLatch

internal class DecodeThread(private val activity: CaptureActivity,
                            decodeFormats:AbstractCollection<BarcodeFormat>?,
                            baseHints: Map<DecodeHintType, Any>?,
                            characterSet: String?,
                            resultPointCallback: ResultPointCallback) : Thread() {
    private val hints: MutableMap<DecodeHintType, Any>
    private var handler: Handler? = null
    private val handlerInitLatch: CountDownLatch

    init {
        Log.i("kang","DecodeThread==init")
        var decodeFormats = decodeFormats
        Log.i("kang","DecodeThread==init1")
        handlerInitLatch = CountDownLatch(1)
        Log.i("kang","DecodeThread==init2")
        hints = EnumMap(DecodeHintType::class.java)
        Log.i("kang","DecodeThread==init3")
        if (baseHints != null) {
            hints.putAll(baseHints)
        }

        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            decodeFormats = EnumSet.noneOf(BarcodeFormat::class.java)
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_1D_PRODUCT, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.PRODUCT_FORMATS)
            }
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_1D_INDUSTRIAL, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS)
            }
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_QR, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.QR_CODE_FORMATS)
            }
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_DATA_MATRIX, true)) {
                decodeFormats!!.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
            }
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_AZTEC, false)) {
                decodeFormats!!.addAll(DecodeFormatManager.AZTEC_FORMATS)
            }
            if (prefs.getBoolean(PreferencesActivity().KEY_DECODE_PDF417, false)) {
                decodeFormats!!.addAll(DecodeFormatManager.PDF417_FORMATS)
            }
        }
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats

        if (characterSet != null) {
            hints[DecodeHintType.CHARACTER_SET] = characterSet
        }
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = resultPointCallback
        Log.i("DecodeThread", "Hints: $hints")
    }

    fun getHandler(): Handler? {
        try {
            handlerInitLatch.await()
        } catch (ie: InterruptedException) {
            // continue?
        }

        return handler
    }

    override fun run() {
        Looper.prepare()
        handler = DecodeHandler(activity, hints)
        handlerInitLatch.countDown()
        Looper.loop()
    }

    companion object {

        val BARCODE_BITMAP = "barcode_bitmap"
        val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"
    }

}