package com.wq.auth.api.controller.member.response

data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiredAt: Long
) {
    companion object {
        fun fromTokens(accessToken: String, refreshToken: String, expiredAt: Long) =
            LoginResponseDto(accessToken, refreshToken, expiredAt)
    }
}
