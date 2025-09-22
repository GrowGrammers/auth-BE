package com.wq.auth.api.controller.auth.response

data class RefreshAccessTokenResponseDto(
    val refreshToken: String?
) {
    companion object {
        fun forApp(refreshToken: String) =
            RefreshAccessTokenResponseDto(refreshToken)
    }
}

