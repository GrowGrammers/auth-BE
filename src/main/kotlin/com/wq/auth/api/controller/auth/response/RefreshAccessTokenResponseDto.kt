package com.wq.auth.api.controller.auth.response

data class RefreshAccessTokenResponseDto(
    val accessToken: String?,
    val refreshToken: String?,
    val expiredAt: Long?,
    val refreshExpiredAt: Long? = null
) {
    companion object {
        fun forWeb(accessToken: String, expiredAt: Long) =
            RefreshAccessTokenResponseDto(accessToken, null, expiredAt, null)

        fun forApp(refreshToken: String, refreshExpiredAt: Long) =
            RefreshAccessTokenResponseDto(null, refreshToken, null, refreshExpiredAt)
    }
}

