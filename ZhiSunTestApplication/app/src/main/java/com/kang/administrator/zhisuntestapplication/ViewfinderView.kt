package com.kang.administrator.zhisuntestapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.zxing.ResultPoint
import com.kang.administrator.zhisuntestapplication.camera.CameraManager
import java.util.ArrayList

class ViewfinderView// This constructor is used when the class is built from an XML resource.
(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var cameraManager: CameraManager? = null
    private val paint: Paint
    private var resultBitmap: Bitmap? = null
    private val maskColor: Int
    private val resultColor: Int
    private val laserColor: Int
    private val resultPointColor: Int
    private var scannerAlpha: Int = 0
    private var possibleResultPoints: MutableList<ResultPoint>? = null
    private var lastPossibleResultPoints: List<ResultPoint>? = null

    init {

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val resources = resources
        maskColor = resources.getColor(R.color.viewfinder_mask)
        resultColor = resources.getColor(R.color.result_view)
        laserColor = resources.getColor(R.color.viewfinder_laser)
        resultPointColor = resources.getColor(R.color.possible_result_points)
        scannerAlpha = 0
        possibleResultPoints = ArrayList(5)
        lastPossibleResultPoints = null
    }

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
    }

    @SuppressLint("DrawAllocation")
    public override fun onDraw(canvas: Canvas) {
        if (cameraManager == null) {
            return  // not ready yet, early draw before done configuring
        }
        val frame = cameraManager!!.getFramingRect()
        val previewFrame = cameraManager!!.getFramingRectInPreview()
        if (frame == null || previewFrame == null) {
            return
        }
        val width = canvas.width
        val height = canvas.height

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = if (resultBitmap != null) resultColor else maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.alpha = CURRENT_POINT_OPACITY
            canvas.drawBitmap(resultBitmap!!, null, frame, paint)
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.color = laserColor
            paint.alpha = SCANNER_ALPHA[scannerAlpha]
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
            val middle = frame.height() / 2 + frame.top
            canvas.drawRect((frame.left + 2).toFloat(), (middle - 1).toFloat(), (frame.right - 1).toFloat(), (middle + 2).toFloat(), paint)

            val scaleX = frame.width() / previewFrame.width().toFloat()
            val scaleY = frame.height() / previewFrame.height().toFloat()

            val currentPossible = possibleResultPoints
            val currentLast = lastPossibleResultPoints
            val frameLeft = frame.left
            val frameTop = frame.top
            if (currentPossible!!.isEmpty()) {
                lastPossibleResultPoints = null
            } else {
                possibleResultPoints = ArrayList(5)
                lastPossibleResultPoints = currentPossible
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor
                synchronized(currentPossible) {
                    for (point in currentPossible) {
                        canvas.drawCircle((frameLeft + (point.x * scaleX).toInt()).toFloat(),
                                (frameTop + (point.y * scaleY).toInt()).toFloat(),
                                POINT_SIZE.toFloat(), paint)
                    }
                }
            }
            if (currentLast != null) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                synchronized(currentLast) {
                    val radius = POINT_SIZE / 2.0f
                    for (point in currentLast) {
                        canvas.drawCircle((frameLeft + (point.x * scaleX).toInt()).toFloat(),
                                (frameTop + (point.y * scaleY).toInt()).toFloat(),
                                radius, paint)
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE)
        }
    }

    fun drawViewfinder() {
        val resultBitmap = this.resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    fun drawResultBitmap(barcode: Bitmap) {
        resultBitmap = barcode
        invalidate()
    }

    fun addPossibleResultPoint(point: ResultPoint) {
        val points = possibleResultPoints
        synchronized(points!!) {
            points!!.add(point)
            val size = points.size
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear()
            }
        }
    }

    companion object {

        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private val ANIMATION_DELAY = 80L
        private val CURRENT_POINT_OPACITY = 0xA0
        private val MAX_RESULT_POINTS = 20
        private val POINT_SIZE = 6
    }

}