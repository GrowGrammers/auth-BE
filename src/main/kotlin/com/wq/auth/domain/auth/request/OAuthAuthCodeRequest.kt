package com.wq.auth.domain.auth.request

data class OAuthAuthCodeRequest(
    val authCode: String,
    val codeVerifier: String,
    val state: String? = null,  // 네이버만 사용
    val redirectUri: String? = null
)
