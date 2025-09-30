package com.wq.auth.api.domain.auth.request

data class NaverSocialLoginRequest(
        val authCode: String,
        val state: String,
        val codeVerifier: String,
)