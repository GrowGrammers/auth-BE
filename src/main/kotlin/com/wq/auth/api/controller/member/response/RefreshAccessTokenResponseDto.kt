package com.wq.auth.api.controller.member.response

data class RefreshAccessTokenResponseDto(
    val accessToken: String,
    val expiredAt: Long
) {
    companion object {
        fun fromTokens(accessToken: String, expiredAt: Long) =
            RefreshAccessTokenResponseDto(accessToken, expiredAt)
    }
}
