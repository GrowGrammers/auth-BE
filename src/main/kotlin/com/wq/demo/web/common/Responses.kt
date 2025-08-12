package com.wq.demo.web.common

import com.wq.demo.shared.error.ApiResponseCode

object Responses {
    fun <T> success(
        message: String = "요청에 성공적으로 응답하였습니다.",
        data: T? = null
    ) : BaseResponse<T> =
        BaseResponse(true, message, data)

    fun fail(code: ApiResponseCode) : BaseResponse<Nothing> =
        BaseResponse(false, code.getMessage(), null)
}