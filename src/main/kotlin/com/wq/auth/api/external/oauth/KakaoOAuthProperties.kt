package com.wq.auth.api.external.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 카카오 OAuth2 설정 프로퍼티
 *
 * application.yml의 app.oauth.kakao 설정을 바인딩합니다.
 *
 * @param clientId 카카오 OAuth2 클라이언트 ID (REST API 키)
 * @param clientSecret 카카오 OAuth2 클라이언트 시크릿
 * @param redirectUri 리다이렉트 URI
 * @param authUri 카카오 OAuth2 인증 URI
 * @param tokenUri 카카오 OAuth2 토큰 발급 URI
 * @param userInfoUri 카카오 사용자 정보 조회 URI
 */
@ConfigurationProperties(prefix = "app.oauth.kakao")
data class KakaoOAuthProperties(
    val clientId: String,
    val clientSecret: String? = null,
    val redirectUri: String,
    val authUri: String,
    val tokenUri: String,
    val userInfoUri: String
)
