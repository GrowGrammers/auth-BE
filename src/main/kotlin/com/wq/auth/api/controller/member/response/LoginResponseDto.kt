package com.wq.auth.api.controller.member.response

data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiredAt: Long
)
