package com.wq.demo.jwt.error

import com.wq.demo.shared.error.ApiException

/**
 * JWT 예외를 도메인 표준 예외로 감싸서 던집니다.
 * GlobalExceptionHandler가 잡아 표준 JSON 포맷으로 내려갑니다.
 */
class JwtException(
    val jwtCode: JwtExceptionCode,
    cause: Throwable? = null
) : ApiException(jwtCode, cause)