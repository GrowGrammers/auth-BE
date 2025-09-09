package com.wq.auth.shared.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.wq.auth.web.common.response.Responses
import com.wq.auth.shared.jwt.error.JwtExceptionCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * JWT 인증 실패 시 처리하는 엔트리포인트
 * : 인증되지 않은 요청(401 Unauthorized)에 대해 표준 JSON 응답을 반환합니다.
 */
@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    private val log = KotlinLogging.logger {}

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        // 로그 출력 (백엔드 전용)
        log.debug { "[401] Authentication failed: ${request.method} ${request.requestURI} - Reason: ${authException.message}" }

        // 응답 설정
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        
        // 표준 API 응답 형식으로 에러 응답 생성
        val errorResponse = Responses.fail(JwtExceptionCode.TOKEN_MISSING)
        val jsonResponse = objectMapper.writeValueAsString(errorResponse)
        
        response.writer.write(jsonResponse)
        response.writer.flush()
    }
}
