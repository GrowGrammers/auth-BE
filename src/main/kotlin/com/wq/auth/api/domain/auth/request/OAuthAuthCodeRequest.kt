package com.wq.auth.api.domain.auth.request

data class OAuthAuthCodeRequest(
    val authCode: String,
    val codeVerifier: String,
    val state: String? = null,  // 네이버만 사용
)
