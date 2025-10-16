package com.wq.auth.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.wq.auth.api.controller.auth.SocialLoginController
import com.wq.auth.api.domain.auth.SocialLinkService
import com.wq.auth.api.domain.auth.SocialLoginService
import com.wq.auth.api.domain.oauth.error.SocialLoginException
import com.wq.auth.api.domain.oauth.error.SocialLoginExceptionCode
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.jwt.JwtProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 소셜 계정 연동 Controller 단위 테스트 (Kotest + Mockito)
 */
@WithMockUser(username = "100", roles = ["USER"])
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

            // --- 성공 케이스 (doNothing 대신 verify 사용) ---
            test("Google 계정 연동 - 신규 연동 성공 (HTTP 200)") {
                // Given: 별도 Mocking 필요 없음 (Unit 반환 기본 동작 사용)
                val requestBody = SocialLinkRequestForTest(authCode = "google_auth_code_123", codeVerifier = "pkce_verifier_123")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("카카오 계정 연동이 완료되었습니다"))

                // 💡 Then: Service 함수가 호출되었는지 검증
                verify(socialLinkService).processSocialLink(any(), any())
            }

            test("네이버 계정 연동 - state 파라미터 포함 성공 (HTTP 200)") {
                // Given: 별도 Mocking 필요 없음
                val requestBody = SocialLinkRequestForTest(authCode = "naver_auth_code_789", state = "random_state_string")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
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
                // 네이버 state 오류는 SocialLoginExceptionCode.NAVER_STATE_MISMATCH 가 더 적절할 수 있습니다.
                whenever(socialLinkService.processSocialLink(any(), any()))
                    .doThrow(SocialLoginException(SocialLoginExceptionCode.NAVER_INVALID_AUTHORIZATION_CODE))

                val requestBody = SocialLinkRequestForTest(authCode = "valid_code", state = "invalid_state")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.message").value(errorMessage))
            }

            test("소셜 계정 연동 - 인증되지 않은 사용자 (HTTP 401)") {
                // Given: @WithMockUser를 사용하지 않고 Authorization 헤더도 없애 401을 유도합니다.
                val requestBody = SocialLinkRequestForTest(authCode = "any_code", state = "any_state")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                )
                    .andExpect(status().isUnauthorized)
            }

            test("소셜 계정 연동 - 유효하지 않은 인가 코드 (HTTP 400 Bad Request)") {
                // Given: Service에서 인가 코드 오류를 던지도록 Mocking
                val errorMessage = "유효하지 않은 인가 코드입니다"
                whenever(socialLinkService.processSocialLink(any(), any()))
                    .doThrow(SocialLoginException(SocialLoginExceptionCode.NAVER_INVALID_AUTHORIZATION_CODE))

                val requestBody = SocialLinkRequestForTest(authCode = "invalid_auth_code", codeVerifier = "any_verifier")

                // When & Then
                mockMvc.perform(
                    post("$baseUri/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
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
    val codeVerifier: String? = null,
    val state: String? = null
)