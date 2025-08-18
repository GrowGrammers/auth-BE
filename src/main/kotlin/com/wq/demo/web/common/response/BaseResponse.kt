package com.wq.demo.web.common.response

sealed interface BaseResponse {
    val success: Boolean
    val message: String
}