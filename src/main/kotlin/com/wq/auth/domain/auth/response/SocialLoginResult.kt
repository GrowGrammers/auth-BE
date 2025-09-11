package com.wq.auth.domain.auth.response

/**
 * 소셜 로그인 서비스 결과 DTO
 * 
 * SocialLoginService에서 컨트롤러로 반환하는 내부용 응답입니다.
 * 토큰 정보만 포함하여 단순화된 구조로 설계되었습니다.
 * 
 * @param accessToken JWT 액세스 토큰
 * @param refreshToken JWT 리프레시 토큰
 */
data class SocialLoginResult(
    val accessToken: String,
    val refreshToken: String
)
