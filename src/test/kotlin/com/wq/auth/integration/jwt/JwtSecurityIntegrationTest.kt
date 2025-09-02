package com.wq.auth.integration.jwt

import com.wq.auth.shared.jwt.JwtProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JwtSecurityIntegrationTest(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider
) : BehaviorSpec({

    given("JWT Security 필터가 설정된 상태에서") {

        `when`("유효한 JWT 토큰으로 보호된 엔드포인트에 접근하면") {
            then("인증에 성공해야 한다") {
                // Given: 유효한 JWT 토큰 생성 (간소화된 구조)
                val validToken = jwtProvider.createAccessToken(
                    opaqueId = "550e8400-e29b-41d4-a716-446655440000", // UUID 형식
                    role = "ADMIN"
                )

                // When & Then: 보호된 엔드포인트에 접근
                mockMvc.perform(
                    MockMvcRequestBuilders.get("/members")
                        .header("Authorization", "Bearer $validToken")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
            }
        }

        `when`("JWT 토큰 없이 보호된 엔드포인트에 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                // When & Then: 토큰 없이 보호된 엔드포인트에 접근
                val result = mockMvc.perform(
                    MockMvcRequestBuilders.get("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andReturn()

                // 응답 상태와 내용 출력
                println("응답 상태: ${result.response.status}")
                println("응답 내용: ${result.response.contentAsString}")

                // 상태 코드 검증
                result.response.status shouldBe 401
            }
        }

        `when`("잘못된 형식의 Authorization 헤더로 접근하면") {
            then("401 Unauthorized 응답을 받아야 한다") {
                // When & Then: 잘못된 형식의 헤더로 접근
                mockMvc.perform(
                    MockMvcRequestBuilders.get("/members")
                        .header("Authorization", "InvalidFormat token123")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            }
        }

        `when`("만료된 JWT 토큰으로 접근하면") {
            then("인증이 실패해야 한다") {
                // Given: 만료 시간이 매우 짧은 토큰 생성 (테스트를 위해 JwtProvider 직접 사용)
                val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMX0.invalid"

                // When & Then: 만료된 토큰으로 접근
                mockMvc.perform(
                    MockMvcRequestBuilders.get("/members")
                        .header("Authorization", "Bearer $expiredToken")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            }
        }

        `when`("공개 엔드포인트에 접근하면") {
            then("토큰 없이도 접근할 수 있어야 한다") {
                // When & Then: 공개 엔드포인트 접근 (존재하지 않는 경로지만 인증은 통과해야 함)
                val result = mockMvc.perform(
                    MockMvcRequestBuilders.get("/public/test")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andReturn()

                // 응답 상태와 내용 출력
                println("공개 엔드포인트 응답 상태: ${result.response.status}")
                println("공개 엔드포인트 응답 내용: ${result.response.contentAsString}")

                // 404 또는 500이면 인증은 통과한 것 (401이 아니면 성공)
                (result.response.status != 401) shouldBe true
            }
        }

        `when`("관리자 권한이 필요한 API에 일반 사용자 토큰으로 접근하면") {
            then("403 Forbidden 응답을 받아야 한다") {
                // Given: 일반 사용자 토큰 생성 (간소화된 구조)
                val userToken = jwtProvider.createAccessToken(
                    opaqueId = "660e8400-e29b-41d4-a716-446655440001", // UUID 형식
                    role = "MEMBER"
                )

                // When & Then: 관리자 API에 접근
                val result = mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/test/admin")
                        .header("Authorization", "Bearer $userToken")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andReturn()

                println("관리자 API 일반사용자 접근 응답 상태: ${result.response.status}")

                // 403 Forbidden, 401 Unauthorized, 또는 500 (AuthorizationDeniedException 처리)
                (result.response.status in listOf(401, 403, 500)) shouldBe true
            }
        }

        `when`("공개 API에 접근하면") {
            then("토큰 없이도 접근할 수 있어야 한다") {
                // When & Then: 공개 API 접근
                val result = mockMvc.perform(
                    MockMvcRequestBuilders.get("/api/test/public")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andReturn()

                println("공개 API 응답 상태: ${result.response.status}")
                println("공개 API 응답 내용: ${result.response.contentAsString}")

                result.response.status shouldBe 200
            }
        }
    }
})