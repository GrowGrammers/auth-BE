package com.wq.auth.api.controller

import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.shared.jwt.JwtProvider
import com.wq.auth.shared.security.annotation.AdminApi
import com.wq.auth.shared.security.annotation.AuthenticatedApi
import com.wq.auth.shared.security.annotation.PublicApi
import com.wq.auth.web.common.response.Responses
import com.wq.auth.web.common.response.SuccessResponse
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
    fun publicTestEndpoint(): SuccessResponse<Map<String, String>> {
        val data = mapOf(
            "endpoint" to "/api/public/test",
            "accessLevel" to "PUBLIC",
            "description" to "누구나 접근 가능한 공개 API"
        )
        return Responses.success("공개 API 접근 성공", data)
    }

    @AuthenticatedApi
    @GetMapping("/api/test")
    fun authenticatedEndpoint(): SuccessResponse<Map<String, String>> {
        val data = mapOf(
            "endpoint" to "/api/test",
            "accessLevel" to "AUTHENTICATED",
            "description" to "로그인한 사용자만 접근 가능한 API"
        )
        return Responses.success("인증된 사용자 API 접근 성공", data)
    }

    @AdminApi
    @GetMapping("/api/admin/test")
    fun adminEndpoint(): SuccessResponse<Map<String, String>> {
        val data = mapOf(
            "endpoint" to "/api/admin/test",
            "accessLevel" to "ADMIN",
            "description" to "관리자 권한이 필요한 API"
        )
        return Responses.success("관리자 API 접근 성공", data)
    }

    @PublicApi
    @GetMapping("/api/public/token")
    fun generateTestToken(
        @RequestParam(defaultValue = "550e8400-e29b-41d4-a716-446655440000") opaqueId: String,
        @RequestParam(defaultValue = "MEMBER") roleString: String
    ): SuccessResponse<Map<String, String>> {
        val role = Role.valueOf(roleString)
        val token = jwtProvider.createAccessToken(opaqueId, role)
        val data = mapOf(
            "token" to token,
            "opaqueId" to opaqueId,
            "role" to role.name,
            "usage" to "Authorization: Bearer $token"
        )
        return Responses.success("JWT 토큰 발급 성공", data)
    }
}
