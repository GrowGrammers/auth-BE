package com.wq.demo.web.common.response

import com.wq.demo.shared.error.ApiResponseCode

object Responses {
    fun <T> success(
        message: String = "요청에 성공적으로 응답하였습니다.",
        data: T? = null
    ): SuccessResponse<T> =
        SuccessResponse(true, message, data)

    fun fail(
        code: ApiResponseCode
    ): FailResponse =
        FailResponse(false, code.message, code.toString())
}