package com.wq.auth.integration.security

import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.security.jwt.JwtProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * JWT 기반 Spring Security 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. 공개 API - 토큰 없이 접근 가능
 * 2. 인증 API - 유효한 토큰 필요
 * 3. 관리자 API - ADMIN 역할 필요
 * 4. JWT 토큰 형식 검증 (Bearer, 만료, 잘못된 형식)
 * 5. 권한별 접근 제어 (401/403 응답)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JwtSpringSecurityIntegrationTest(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider
) : BehaviorSpec({

    given("JWT 기반 Spring Security 시스템에서") {

        `when`("공개 API에 토큰 없이 접근하면") {
            then("200 OK 응답을 받아야 한다") {
                val result = mockMvc.perform(
                    get("/api/public/test")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 200
            }
        }

        `when`("인증된 사용자 API에 유효한 MEMBER 토큰으로 접근하면") {
            then("200 OK 응답을 받아야 한다") {
                // Given: MEMBER 역할 토큰 생성
                val memberToken = jwtProvider.createAccessToken(
                    opaqueId = "550e8400-e29b-41d4-a716-446655440000",
                    role = Role.MEMBER
                )

                val result = mockMvc.perform(
                    get("/api/test")
                        .header("Authorization", "Bearer $memberToken")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 200
            }
        }

        `when`("인증된 사용자 API에 유효한 ADMIN 토큰으로 접근하면") {
            then("200 OK 응답을 받아야 한다") {
                // Given: ADMIN 역할 토큰 생성
                val adminToken = jwtProvider.createAccessToken(
                    opaqueId = "660e8400-e29b-41d4-a716-446655440001",
                    role = Role.ADMIN
                )

                val result = mockMvc.perform(
                    get("/api/test")
                        .header("Authorization", "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 200
            }
        }

        `when`("관리자 API에 ADMIN 토큰으로 접근하면") {
            then("200 OK 응답을 받아야 한다") {
                // Given: ADMIN 역할 토큰 생성
                val adminToken = jwtProvider.createAccessToken(
                    opaqueId = "660e8400-e29b-41d4-a716-446655440001",
                    role = Role.ADMIN
                )

                val result = mockMvc.perform(
                    get("/api/admin/test")
                        .header("Authorization", "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 200
            }
        }

        `when`("관리자 API에 MEMBER 토큰으로 접근하면") {
            then("403 Forbidden 응답을 받아야 한다") {
                // Given: MEMBER 역할 토큰 생성
                val memberToken = jwtProvider.createAccessToken(
                    opaqueId = "550e8400-e29b-41d4-a716-446655440000",
                    role = Role.MEMBER
                )

                val result = mockMvc.perform(
                    get("/api/admin/test")
                        .header("Authorization", "Bearer $memberToken")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 403
            }
        }

        `when`("인증된 사용자 API에 토큰 없이 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                val result = mockMvc.perform(
                    get("/api/authenticated/test")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 401
            }
        }

        `when`("잘못된 JWT 토큰으로 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                val result = mockMvc.perform(
                    get("/api/authenticated/test")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 401
            }
        }

        `when`("Bearer 없는 토큰으로 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                val memberToken = jwtProvider.createAccessToken(
                    opaqueId = "550e8400-e29b-41d4-a716-446655440000",
                    role = Role.MEMBER
                )

                val result = mockMvc.perform(
                    get("/api/authenticated/test")
                        .header("Authorization", memberToken) // Bearer 없이
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 401
            }
        }

        `when`("만료된 JWT 토큰으로 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                // Given: 만료된 토큰 (과거 시간으로 설정)
                val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMX0.invalid"

                val result = mockMvc.perform(
                    get("/api/authenticated/test")
                        .header("Authorization", "Bearer $expiredToken")
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn()

                result.response.status shouldBe 401
            }
        }
    }
})
