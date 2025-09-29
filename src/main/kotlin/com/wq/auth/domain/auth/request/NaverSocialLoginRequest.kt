package com.wq.auth.domain.auth.request

data class NaverSocialLoginRequest(
        val authCode: String,
        val state: String,
        val grantType: String,
        val redirectUri: String? = null
)