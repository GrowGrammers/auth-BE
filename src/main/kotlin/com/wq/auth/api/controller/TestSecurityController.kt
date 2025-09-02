package com.wq.auth.api.controller

import com.wq.auth.shared.jwt.JwtProvider
import com.wq.auth.shared.security.annotation.AdminApi
import com.wq.auth.shared.security.annotation.AuthenticatedApi
import com.wq.auth.shared.security.annotation.PublicApi
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Security 테스트용 컨트롤러
 * 개발 및 테스트 환경에서 JWT 인증/인가 동작을 확인하기 위한 엔드포인트 제공
 * todo: 나중에 제거 예정
 */
@RestController
class TestSecurityController(
    private val jwtProvider: JwtProvider
) {

    @PublicApi
    @GetMapping("/api/public/test")
    fun publicTestEndpoint(): Map<String, String> {
        return mapOf("message" to "공개 API 접근 성공")
    }

    @AuthenticatedApi
    @GetMapping("/api/authenticated/test")
    fun authenticatedEndpoint(): Map<String, String> {
        return mapOf("message" to "인증된 사용자 API 접근 성공")
    }

    @AdminApi
    @GetMapping("/api/admin/test")
    fun adminEndpoint(): Map<String, String> {
        return mapOf("message" to "관리자 API 접근 성공")
    }

    @PublicApi
    @GetMapping("/api/public/token")
    fun generateTestToken(
        @RequestParam(defaultValue = "550e8400-e29b-41d4-a716-446655440000") opaqueId: String,
        @RequestParam(defaultValue = "MEMBER") role: String
    ): Map<String, String> {
        val token = jwtProvider.createAccessToken(opaqueId, role)
        return mapOf(
            "token" to token,
            "opaqueId" to opaqueId,
            "role" to role,
            "usage" to "Authorization: Bearer $token"
        )
    }
}
