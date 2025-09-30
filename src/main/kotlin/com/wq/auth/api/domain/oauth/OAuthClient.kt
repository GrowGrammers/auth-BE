package com.wq.auth.api.domain.oauth

import com.wq.auth.api.domain.auth.request.OAuthAuthCodeRequest

/**
 * OAuth 클라이언트 포트 인터페이스
 * 
 * 소셜 로그인 제공자와의 통신을 추상화한 포트입니다.
 * 구체적인 OAuth 제공자(Google, Kakao, Naver 등)의 구현체는 이 인터페이스를 구현합니다.
 * 
 * 도메인 레이어는 이 포트를 통해서만 외부 OAuth 제공자와 통신하므로,
 * 외부 API 변경이나 새로운 제공자 추가 시에도 도메인 로직은 영향을 받지 않습니다.
 */
interface OAuthClient {

    /**
     * 인가 코드를 사용하여 사용자 정보를 조회합니다.
     *
     * @param authCode 소셜 제공자로부터 받은 인가 코드
     * @param state CSRF 방지용 상태 값 (네이버)
     * @param codeVerifier PKCE 검증용 코드 검증자
     * @return 도메인 사용자 정보
     */
    fun getUserFromAuthCode(
        request: OAuthAuthCodeRequest
    ): OAuthUser
}
