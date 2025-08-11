package com.wq.demo.web.common

import com.wq.demo.shared.error.ApiCode

object Responses {
    fun <T> success(
        message: String = "요청에 성공적으로 응답하였습니다.",
        data: T? = null
    ) : BaseResponse<T> =
        BaseResponse(true, message, data, null)

    fun fail(code: ApiCode) : BaseResponse<Nothing> =
        BaseResponse(false, code.getMessage(), null, code.toString())
}