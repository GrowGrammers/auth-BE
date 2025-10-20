package com.wq.auth.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.wq.auth.api.controller.auth.SocialLoginController
import com.wq.auth.api.domain.auth.SocialLinkService
import com.wq.auth.api.domain.auth.SocialLoginService
import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.api.domain.oauth.error.SocialLoginException
import com.wq.auth.api.domain.oauth.error.SocialLoginExceptionCode
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.principal.PrincipalDetails
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import jakarta.servlet.http.Cookie
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 소셜 계정 연동 Controller 단위 테스트 (Kotest + Mockito)
 */
@WebMvcTest(SocialLoginController::class)
class SocialLinkControllerTest : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var socialLinkService: SocialLinkService

    @MockitoBean
    lateinit var socialLoginService: SocialLoginService

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var jwtProvider: JwtProvider

    init {
        context("POST /api/v1/auth/link/{provider}") {

            val baseUri = "/api/v1/auth/link"
            val refreshToken = "valid-refresh-token"
            val clientType = "web"
            val accessToken = "access-token"

            val principal = PrincipalDetails(
                opaqueId = "100",
                role = Role.MEMBER
            )

            test("Google 계정 연동 - 신규 연동 성공 (HTTP 200)") {
                // Given
                val requestBody = SocialLinkRequestForTest(authCode = "google_auth_code_123", codeVerifier = "pkce_verifier_123")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/google")
                        .header("Authorization", "Bearer $accessToken")
                        .cookie(Cookie("refreshToken", refreshToken))
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                        .with(user(principal))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Google 계정 연동이 완료되었습니다"))

                // 💡 Then: Service 함수가 호출되었는지 검증 (doNothing 대체)
                verify(socialLinkService).processSocialLink(any(), any())
            }

            test("카카오 계정 연동 - 병합 성공 (HTTP 200)") {
                // Given: 별도 Mocking 필요 없음
                val requestBody = SocialLinkRequestForTest(authCode = "kakao_auth_code_456", codeVerifier = "pkce_verifier_456")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/kakao")
                        .header("Authorization", "Bearer $accessToken")
                        .cookie(Cookie("refreshToken", refreshToken))
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                        .with(user(principal))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("카카오 계정 연동이 완료되었습니다"))

                // 💡 Then: Service 함수가 호출되었는지 검증
                verify(socialLinkService).processSocialLink(any(), any())
            }

            test("네이버 계정 연동 - state 파라미터 포함 성공 (HTTP 200)") {
                // Given: 별도 Mocking 필요 없음
                val requestBody = SocialLinkRequestForTest(authCode = "naver_auth_code_789", state = "random_state_string", codeVerifier = "any_code")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .header("Authorization", "Bearer $accessToken")
                        .cookie(Cookie("refreshToken", refreshToken))
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                        .with(user(principal))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("네이버 계정 연동이 완료되었습니다"))

                // 💡 Then: Service 함수가 호출되었는지 검증
                verify(socialLinkService).processSocialLink(any(), any())
            }

            // --- 실패 케이스 (doThrow 사용) ---

            test("네이버 계정 연동 - state 불일치/만료로 실패 (HTTP 400 Bad Request)") {
                // Given: Service에서 오류를 던지도록 Mocking
                val errorMessage = "유효하지 않은 네이버 state 값입니다"

                whenever(socialLinkService.processSocialLink(any(), any()))
                    .doThrow(SocialLoginException(SocialLoginExceptionCode.NAVER_INVALID_STATE))

                val requestBody = SocialLinkRequestForTest(authCode = "valid_code", state = "invalid_state", codeVerifier = "any_code")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .header("Authorization", "Bearer $accessToken")
                        .cookie(Cookie("refreshToken", refreshToken))
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                        .with(user(principal))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.message").value(errorMessage))
            }

            test("소셜 계정 연동(네이버) - 인증되지 않은 사용자 (HTTP 401)") {
                // Given: @WithMockUser를 사용하지 않고 Authorization 헤더도 없애 401을 유도합니다.
                val requestBody = SocialLinkRequestForTest(authCode = "any_code", state = "any_state", codeVerifier = "any_code")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                )
                    .andExpect(status().isUnauthorized)
            }

            test("소셜 계정 연동(구글) - 유효하지 않은 인가 코드 (HTTP 400 Bad Request)") {
                // Given: Service에서 인가 코드 오류를 던지도록 Mocking
                val errorMessage = "유효하지 않은 Google 인가 코드입니다"
                whenever(socialLinkService.processSocialLink(any(), any()))
                    .doThrow(SocialLoginException(SocialLoginExceptionCode.GOOGLE_INVALID_AUTHORIZATION_CODE))

                val requestBody = SocialLinkRequestForTest(authCode = "invalid_auth_code", codeVerifier = "any_verifier")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/google")
                        .header("Authorization", "Bearer $accessToken")
                        .cookie(Cookie("refreshToken", refreshToken))
                        .header("X-Client-Type", clientType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()) //security 우회용
                        .with(user(principal))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.message").value(errorMessage))
            }
        }
    }
}

// 테스트에서 사용하는 DTO (요청 본문 구조에 맞게 정의)
data class SocialLinkRequestForTest(
    val authCode: String,
    val codeVerifier: String,
    val state: String? = null
)