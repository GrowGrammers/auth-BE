package com.wq.demo.web.common.response

data class SuccessResponse<T>(
    override val success: Boolean = true,
    override val message: String = "OK",
    val data: T? = null
) : BaseResponse