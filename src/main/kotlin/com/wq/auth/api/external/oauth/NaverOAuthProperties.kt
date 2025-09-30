package com.wq.auth.api.external.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Naver OAuth2 설정 프로퍼티
 *
 * application.yml의 app.oauth.naver 설정을 바인딩합니다.
 *
 * @param clientId Naver OAuth2 클라이언트 ID
 * @param clientSecret Naver OAuth2 클라이언트 시크릿
 * @param redirectUri 리다이렉트 URI
 * @param authUri Naver OAuth2 인증 URI
 * @param tokenUri Naver OAuth2 토큰 발급 URI
 * @param userInfoUri Naver 사용자 정보 조회 URI
 */
@ConfigurationProperties(prefix = "app.oauth.naver")
data class NaverOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val authUri: String,
    val tokenUri: String,
    val userInfoUri: String
)
