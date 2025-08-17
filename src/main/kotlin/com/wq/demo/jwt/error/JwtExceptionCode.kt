package com.wq.demo.jwt.error

import com.wq.demo.shared.error.ApiResponseCode


/**
 * JWT 관련 오류 코드. GlobalExceptionHandler에서 표준 응답으로 직렬화됩니다.
 */
enum class JwtExceptionCode (
    private val status: Int,
    private val message: String
) : ApiResponseCode {
    TOKEN_MISSING(400, "인증 토큰이 없습니다."),
    TOKEN_FORMAT_INVALID(400, "Authorization 헤더는 'Bearer <token>' 형식이어야 합니다."),
    INVALID_SIGNATURE(401, "유효하지 않은 JWT 서명입니다."),
    MALFORMED(401, "유효하지 않은 JWT 토큰입니다."),
    EXPIRED(401, "만료된 JWT 토큰입니다."),
    UNSUPPORTED(401, "지원되지 않는 JWT 토큰입니다.");

    override fun getStatus(): Int = status
    override fun getMessage(): String = message
}