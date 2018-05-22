package com.kang.administrator.zhisuntestapplication.result

import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.kang.administrator.zhisuntestapplication.CaptureActivity

object ResultHandlerFactory {

    fun makeResultHandler(activity: CaptureActivity, rawResult: Result): ResultHandler {
        val result = parseResult(rawResult)

        return TextResultHandler(activity, result, rawResult)
    }

    private fun parseResult(rawResult: Result): ParsedResult {
        return ResultParser.parseResult(rawResult)
    }
}