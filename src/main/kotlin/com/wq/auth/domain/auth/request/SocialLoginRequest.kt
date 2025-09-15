package com.wq.auth.domain.auth.request

import com.wq.auth.api.domain.auth.entity.ProviderType

data class SocialLoginRequest(
    val authCode: String,
    val codeVerifier: String,
    val providerType: ProviderType,
    val redirectUri: String? = null
)