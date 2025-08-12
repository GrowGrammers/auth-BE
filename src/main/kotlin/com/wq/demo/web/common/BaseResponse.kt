package com.wq.demo.web.common

data class BaseResponse<T> (
    val success: Boolean,
    val message: String,
    val data: T?
)