package com.wq.auth.api.domain.auth.request

import com.wq.auth.api.domain.auth.entity.ProviderType

/**
 * 소셜 계정 연동 요청 도메인 모델
 */
data class SocialLinkRequest(
    val authCode: String,
    val codeVerifier: String,
    val state: String? = null,
    val providerType: ProviderType,
)