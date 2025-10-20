package com.wq.auth.api.domain.auth.request

data class EmailLoginLinkRequest(
    val email: String,
    val verifyCode: String,
)