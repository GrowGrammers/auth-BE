package com.wq.auth.api.controller.auth.response

data class LoginResponseDto(
    val refreshToken: String?,
) {
    companion object {
        fun forApp(refreshToken: String) =
            LoginResponseDto(refreshToken)
    }
}