package com.wq.auth.domain.oauth.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Google OAuth2 설정 프로퍼티
 *
 * application.yml의 app.oauth.google 설정을 바인딩합니다.
 *
 * @param clientId Google OAuth2 클라이언트 ID
 * @param clientSecret Google OAuth2 클라이언트 시크릿
 * @param redirectUri 리다이렉트 URI
 * @param authUri Google OAuth2 인증 URI
 * @param tokenUri Google OAuth2 토큰 발급 URI
 * @param userInfoUri Google 사용자 정보 조회 URI
 */
@ConfigurationProperties(prefix = "app.oauth.google")
data class GoogleOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val authUri: String,
    val tokenUri: String,
    val userInfoUri: String
)