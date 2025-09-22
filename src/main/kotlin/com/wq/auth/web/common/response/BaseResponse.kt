package com.wq.auth.web.common.response

sealed interface BaseResponse {
    val success: Boolean
    val message: String
}