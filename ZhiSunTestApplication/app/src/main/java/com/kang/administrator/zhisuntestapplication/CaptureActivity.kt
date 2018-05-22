package com.kang.administrator.zhisuntestapplication

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.kang.administrator.zhisuntestapplication.DecodeFormatManager.parseDecodeFormats
import com.kang.administrator.zhisuntestapplication.IntentSource.ZXING_LINK
import com.kang.administrator.zhisuntestapplication.camera.CameraManager
import com.kang.administrator.zhisuntestapplication.camera.FrontLightMode
import com.kang.administrator.zhisuntestapplication.result.ResultHandler
import com.kang.administrator.zhisuntestapplication.result.ResultHandlerFactory
import kotlinx.android.synthetic.main.activity_capture.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.io.IOException
import java.net.URL
import java.text.DateFormat
import java.util.*

class CaptureActivity : Activity() ,SurfaceHolder.Callback{


    private val TAG = CaptureActivity::class.java.simpleName

    private val DEFAULT_INTENT_RESULT_DURATION_MS = 1500L
    private val BULK_MODE_SCAN_DELAY_MS = 1000L

    private val ZXING_URLS = arrayOf("http://zxing.appspot.com/scan", "zxing://scan/")

    private val HISTORY_REQUEST_CODE = 0x0000bacc

    private val DISPLAYABLE_METADATA_TYPES = EnumSet.of(ResultMetadataType.ISSUE_NUMBER,
            ResultMetadataType.SUGGESTED_PRICE,
            ResultMetadataType.ERROR_CORRECTION_LEVEL,
            ResultMetadataType.POSSIBLE_COUNTRY)

    var cameraManager: CameraManager? = CameraManager(this)
    var handler: CaptureActivityHandler? = null

    private var inactivityTimer: InactivityTimer= InactivityTimer(this);
    private var lastResult: Result? = null
    private var source: IntentSource? = null

    private var decodeFormats: AbstractCollection<BarcodeFormat>? = null
    private var decodeHints: Map<DecodeHintType, Any>? = null
    private var characterSet: String? = null
    private var savedResultToShow: Result? = null
    private var hasSurface: Boolean = false

    private lateinit var beepManager: BeepManager
    private var ambientLightManager = AmbientLightManager(this)

    override fun onPause() {
        super.onPause()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        beepManager=BeepManager(this)
        hasSurface = false
    }

    override fun onResume() {
        super.onResume()
        viewfinderView.setCameraManager(cameraManager!!)

        handler = null
        lastResult = null


        beepManager.updatePrefs()
        ambientLightManager.start(cameraManager!!)

        inactivityTimer.onResume()

        val intent = intent


        source = IntentSource.NONE
        decodeFormats = null
        characterSet = null

        if (intent != null) {

            val action = intent.action
            val dataString = intent.dataString

            if (Intents.Scan.ACTION.equals(action)) {
                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT
                decodeFormats = parseDecodeFormats(intent) as AbstractCollection<BarcodeFormat>
                decodeHints = DecodeHintManager.parseDecodeHints(intent)

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    val width = intent.getIntExtra(Intents.Scan.WIDTH, 0)
                    val height = intent.getIntExtra(Intents.Scan.HEIGHT, 0)
                    if (width > 0 && height > 0) {
                        cameraManager!!.setManualFramingRect(width, height)
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    val cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1)
                    if (cameraId >= 0) {
                        cameraManager!!.setManualCameraId(cameraId)
                    }
                }


            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK
                //sourceUrl = dataString
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS as AbstractCollection<BarcodeFormat>

            } else if (isZXingURL(dataString)) {

                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK
                //sourceUrl = dataString
                val inputUri = Uri.parse(dataString)
                //scanFromWebPageManager = ScanFromWebPageManager(inputUri)
                decodeFormats = parseDecodeFormats(inputUri) as AbstractCollection<BarcodeFormat>
                // Allow a sub-set of the hints to be specified by the caller.
                decodeHints = DecodeHintManager.parseDecodeHints(inputUri) as Map<DecodeHintType, Any>?

            }

            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)

        }

        val surfaceHolder = preview_view.holder
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder)
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this)
        }
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
            if (handler == null) {
                handler = CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager!!)
            }
            decodeOrStoreSavedBitmap(null, null)
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
        } catch (e: RuntimeException) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e)
        }

    }

    private fun decodeOrStoreSavedBitmap(bitmap: Bitmap?, result: Result?) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result
        } else {
            if (result != null) {
                savedResultToShow = result
            }
            if (savedResultToShow != null) {
                val message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow)
                handler!!.sendMessage(message)
            }
            savedResultToShow = null
        }
    }


    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {

        if (p0 == null) {
        }
        if (!hasSurface) {
            hasSurface = true
            initCamera(p0)
        }
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    fun handleDecode(rawResult: Result, barcode: Bitmap?, scaleFactor: Float) {
        inactivityTimer.onActivity()
        lastResult = rawResult

        val resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult)
        val codestr=rawResult.text
        contents_text_view.text=codestr
    }

    fun drawViewfinder() {
        viewfinderView.drawViewfinder()
    }

    private fun isZXingURL(dataString: String?): Boolean {
        if (dataString == null) {
            return false
        }
        for (url in ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true
            }
        }
        return false
    }

    data class StartReceive(var code:String,var message:String)

}
