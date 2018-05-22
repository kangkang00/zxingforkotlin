package com.kang.administrator.zhisuntestapplication.result

import android.app.Activity
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.kang.administrator.zhisuntestapplication.R

class TextResultHandler(activity: Activity, result: ParsedResult, rawResult: Result) : ResultHandler(activity, result, rawResult) {

    override val buttonCount: Int
        get() = if (hasCustomProductSearch()) buttons.size else buttons.size - 1

    override val displayTitle: Int
        get() = R.string.result_text

    override fun getButtonText(index: Int): Int {
        return buttons[index]
    }

    override fun handleButtonPress(index: Int) {
        val text = result.getDisplayResult()
        when (index) {
            0 -> webSearch(text)
            1 -> shareByEmail(text)
            2 -> shareBySMS(text)
            3 -> openURL(fillInCustomSearchURL(text))
        }
    }

    companion object {

        private val buttons = intArrayOf(R.string.button_web_search, R.string.button_share_by_email, R.string.button_share_by_sms, R.string.button_custom_product_search)
    }
}