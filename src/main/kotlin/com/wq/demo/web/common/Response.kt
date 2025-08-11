package com.wq.demo.web.common

data class BaseResponse<T> (
    val success: Boolean,
    val message: String,
    val data: T?,
    val error: String?
)

object Responses {
    fun <T> success(message: String) : BaseResponse<T> =
        BaseResponse(true, message, null, null)

    fun <T> success(data: T?) : BaseResponse<T> =
         BaseResponse(true, "요청에 성공하였습니다.", data, null)

    fun <T> success(message: String, data: T?) : BaseResponse<T> =
        BaseResponse(true, message, data, null)

    fun <T> fail(message: String) : BaseResponse<T> =
        BaseResponse(false, message, null, null)
}