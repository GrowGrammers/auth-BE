package com.wq.auth.domain.oauth.error

import com.wq.auth.shared.error.ApiException

/**
 * 소셜 로그인 관련 예외
 * 
 * 기존 ApiException 구조를 따라 GlobalExceptionHandler에서 
 * 표준 JSON 응답으로 처리됩니다.
 */
class SocialLoginException(
    val socialLoginCode: SocialLoginExceptionCode,
    cause: Throwable? = null
) : ApiException(socialLoginCode, cause)
