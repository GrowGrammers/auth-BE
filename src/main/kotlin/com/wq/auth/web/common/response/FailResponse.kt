package com.wq.auth.web.common.response

import io.swagger.v3.oas.annotations.media.Schema

data class FailResponse(
    @Schema(description = "요청 성공 여부", example = "false")
    override val success: Boolean = false,

    @Schema(description = "에러 메시지")
    override val message: String,

    @Schema(description = "에러 코드")
    val error: String               // enum 에러코드.toString() 값을 사용.
) : BaseResponse
