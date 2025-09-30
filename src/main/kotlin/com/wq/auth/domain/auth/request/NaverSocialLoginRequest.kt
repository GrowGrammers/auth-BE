package com.wq.auth.domain.auth.request

data class NaverSocialLoginRequest(
        val authCode: String,
        val state: String,
        val codeVerifier: String,
        val redirectUri: String? = null
)