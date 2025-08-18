package com.wq.demo.web.common.response

data class FailResponse(
    override val success: Boolean = false,
    override val message: String,
    val error: String               // enum 에러코드.toString() 값을 사용.
) : BaseResponse
