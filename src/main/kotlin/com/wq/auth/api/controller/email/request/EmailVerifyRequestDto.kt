package com.wq.auth.api.controller.email.request

data class EmailVerifyRequestDto (val email: String, val verifyCode: String)