package com.wq.auth.api.controller.auth.response

data class LoginResponseDto(
    val accessToken: String?,
    val refreshToken: String?,
    val expiredAt: Long?,
    val refreshExpiredAt: Long? = null
) {
    companion object {
        fun forWeb(accessToken: String, expiredAt: Long) =
            LoginResponseDto(accessToken, null, expiredAt, null)

        fun forApp(refreshToken: String, refreshExpiredAt: Long) =
            LoginResponseDto(null, refreshToken, null, refreshExpiredAt)
    }
}