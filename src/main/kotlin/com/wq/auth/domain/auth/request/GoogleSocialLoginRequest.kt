package com.wq.auth.domain.auth.request

data class GoogleSocialLoginRequest(
    val authCode: String,
    val codeVerifier: String,
    val redirectUri: String? = null
)