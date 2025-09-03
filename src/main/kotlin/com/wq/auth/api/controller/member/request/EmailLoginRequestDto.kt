package com.wq.auth.api.controller.member.request

data class EmailLoginRequestDto (val email: String, val verifyCode: String)
