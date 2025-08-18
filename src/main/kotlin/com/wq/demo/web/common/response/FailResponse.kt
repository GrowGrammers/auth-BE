package com.wq.demo.web.common.response

data class FailResponse(
    override val success: Boolean = false,
    override val message: String,
    val error: String
) : BaseResponse
