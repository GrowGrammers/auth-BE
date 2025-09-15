package com.wq.auth.api.controller.auth.request

data class EmailLoginRequestDto (val email: String, val verifyCode: String, val deviceId: String?)
