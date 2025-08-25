package com.wq.auth.web.common.response

import io.swagger.v3.oas.annotations.media.Schema

data class SuccessResponse<T>(
    @Schema(description = "요청 성공 여부", example = "true")
    override val success: Boolean = true,

    @Schema(description = "응답 메시지", example = "OK")
    override val message: String = "OK",

    @Schema(description = "응답 데이터 예시")
    val data: T? = null
) : BaseResponse