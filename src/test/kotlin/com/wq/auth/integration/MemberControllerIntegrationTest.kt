package com.wq.auth.integration

import com.wq.auth.api.controller.member.MemberController
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.jwt.error.JwtException
import com.wq.auth.jwt.error.JwtExceptionCode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringTestExtension
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import java.time.Duration

@WebMvcTest(
    controllers = [MemberController::class],
    //excludeAutoConfiguration = [org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class]
)
class MemberControllerIntegrationTest : DescribeSpec() {

    override fun extensions() = listOf(SpringTestExtension())

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var memberService: MemberService

    @MockitoBean
    lateinit var authEmailService: AuthEmailService

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var jwtProvider: JwtProvider

    init {
        describe("POST /api/v1/auth/members/refresh") {

            context("Web 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답과 새로운 토큰을 반환해야 한다") {
                    // given
                    val refreshToken = "valid-refresh-token"
                    val clientType = "web"
                    val deviceId: String? = null
                    val newAccessToken = "new-access-token"
                    val newRefreshToken = "new-refresh-token"
                    val accessTokenExpiredAt = System.currentTimeMillis() + 3600000
                    val refreshTokenExpiredAt = System.currentTimeMillis() + (7 * 24 * 3600000)

                    val tokenResult = MemberService.TokenResult(
                        newAccessToken,
                        newRefreshToken,
                        accessTokenExpiredAt,
                        refreshTokenExpiredAt
                    )
                    given(memberService.refreshAccessToken(refreshToken, deviceId, clientType))
                        .willReturn(tokenResult)
                    given(jwtProperties.refreshExp).willReturn(Duration.ofDays(7))

                    val requestBody = """{"deviceId": null}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .cookie(Cookie("refreshToken", refreshToken))
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("AccessToken 재발급에 성공했습니다."))
                        .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
                        .andExpect(jsonPath("$.data.expiredAt").value(accessTokenExpiredAt))
                        .andExpect(
                            header().string(
                                "Set-Cookie",
                                Matchers.containsString("refreshToken=$newRefreshToken")
                            )
                        )
                        .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")))
                        .andExpect(header().string("Set-Cookie", Matchers.containsString("SameSite=Lax")))

                    verify(memberService).refreshAccessToken(refreshToken, deviceId, clientType)
                }
            }

            context("App 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답과 새로운 토큰을 반환해야 한다") {
                    // given
                    val refreshToken = "valid-refresh-token"
                    val clientType = "app"
                    val deviceId = "device123"
                    val newAccessToken = "new-access-token"
                    val newRefreshToken = "new-refresh-token"
                    val accessTokenExpiredAt = System.currentTimeMillis() + 3600000
                    val refreshTokenExpiredAt = System.currentTimeMillis() + (7 * 24 * 3600000)

                    val tokenResult = MemberService.TokenResult(
                        newAccessToken,
                        newRefreshToken,
                        accessTokenExpiredAt,
                        refreshTokenExpiredAt
                    )
                    given(memberService.refreshAccessToken(refreshToken, deviceId, clientType))
                        .willReturn(tokenResult)

                    val requestBody = """{"refreshToken": "$refreshToken", "deviceId": "$deviceId"}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("AccessToken 재발급에 성공했습니다."))
                        .andExpect(jsonPath("$.data.refreshToken").value(newRefreshToken))
                        .andExpect(jsonPath("$.data.refreshExpiredAt").value(refreshTokenExpiredAt))
                        .andExpect(header().doesNotExist("Set-Cookie"))

                    verify(memberService).refreshAccessToken(refreshToken, deviceId, clientType)
                }
            }

            context("Web 클라이언트에서 서비스에서 예외가 발생했을 때") {
                it("적절한 에러 응답을 반환해야 한다") {
                    // given
                    val refreshToken = "invalid-refresh-token"
                    val clientType = "web"
                    val deviceId: String? = null
                    val requestBody = """{"deviceId": null}"""

                    given(memberService.refreshAccessToken(refreshToken, deviceId, clientType))
                        .willThrow(JwtException(JwtExceptionCode.MALFORMED))

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .cookie(Cookie("refreshToken", refreshToken))
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    ).andExpect(status().isUnauthorized)

                    verify(memberService).refreshAccessToken(refreshToken, deviceId, clientType)
                }
            }

            context("App 클라이언트에서 서비스에서 예외가 발생했을 때") {
                it("적절한 에러 응답을 반환해야 한다") {
                    // given
                    val refreshToken = "invalid-refresh-token"
                    val clientType = "app"
                    val deviceId = "device123"
                    val requestBody = """{"refreshToken": "$refreshToken", "deviceId": "$deviceId"}"""

                    given(memberService.refreshAccessToken(refreshToken, deviceId, clientType))
                        .willThrow(JwtException(JwtExceptionCode.EXPIRED))

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    ).andExpect(status().isUnauthorized)

                    verify(memberService).refreshAccessToken(refreshToken, deviceId, clientType)
                }
            }
        }

        describe("POST /api/v1/auth/members/email-login") {

            context("Web 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답과 토큰을 반환해야 한다") {
                    // given
                    val email = "test@example.com"
                    val verifyCode = "123456"
                    val clientType = "web"
                    val deviceId: String? = null
                    val accessToken = "access-token"
                    val refreshToken = "refresh-token"
                    val accessTokenExpiredAt = System.currentTimeMillis() + 3600000
                    val refreshTokenExpiredAt = System.currentTimeMillis() + (7 * 24 * 3600000)

                    val tokenResult = MemberService.TokenResult(
                        accessToken,
                        refreshToken,
                        accessTokenExpiredAt,
                        refreshTokenExpiredAt
                    )

                    given(memberService.emailLogin(email, deviceId, clientType)).willReturn(tokenResult)
                    given(jwtProperties.refreshExp).willReturn(Duration.ofDays(7))

                    val requestBody = """{"email": "$email", "verifyCode": "$verifyCode", "deviceId": null}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/email-login")
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                        .andExpect(jsonPath("$.data.accessToken").value(accessToken))
                        .andExpect(jsonPath("$.data.expiredAt").value(accessTokenExpiredAt))
                        .andExpect(
                            header().string(
                                "Set-Cookie",
                                Matchers.containsString("refreshToken=$refreshToken")
                            )
                        )

                    verify(authEmailService).verifyCode(email, verifyCode)
                    verify(memberService).emailLogin(email, deviceId, clientType)
                }
            }

            context("App 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답과 토큰을 반환해야 한다") {
                    // given
                    val email = "test@example.com"
                    val verifyCode = "123456"
                    val clientType = "app"
                    val deviceId = "device123"
                    val accessToken = "access-token"
                    val refreshToken = "refresh-token"
                    val accessTokenExpiredAt = System.currentTimeMillis() + 3600000
                    val refreshTokenExpiredAt = System.currentTimeMillis() + (7 * 24 * 3600000)

                    val tokenResult = MemberService.TokenResult(
                        accessToken,
                        refreshToken,
                        accessTokenExpiredAt,
                        refreshTokenExpiredAt
                    )

                    given(memberService.emailLogin(email, deviceId, clientType)).willReturn(tokenResult)

                    val requestBody = """{"email": "$email", "verifyCode": "$verifyCode", "deviceId": "$deviceId"}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/email-login")
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                        .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
                        .andExpect(jsonPath("$.data.refreshExpiredAt").value(refreshTokenExpiredAt))
                        .andExpect(header().doesNotExist("Set-Cookie"))

                    verify(authEmailService).verifyCode(email, verifyCode)
                    verify(memberService).emailLogin(email, deviceId, clientType)
                }
            }
        }

        describe("POST /api/v1/auth/members/logout") {

            context("Web 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답과 쿠키 삭제를 반환해야 한다") {
                    // given
                    val refreshToken = "valid-refresh-token"
                    val accessToken = "valid-access-token"
                    val clientType = "web"

                    val requestBody = """{}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/logout")
                            .cookie(Cookie("refreshToken", refreshToken))
                            .header("AccessToken", accessToken)
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."))
                        .andExpect(jsonPath("$.data").isEmpty)
                        .andExpect(
                            header().string(
                                "Set-Cookie",
                                Matchers.containsString("refreshToken=")
                            )
                        )
                        .andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")))

                    verify(memberService).logout(refreshToken)
                    verify(jwtProvider).validateOrThrow(accessToken)
                }
            }

            context("App 클라이언트에서 유효한 요청이 주어졌을 때") {
                it("성공 응답을 반환해야 한다") {
                    // given
                    val refreshToken = "valid-refresh-token"
                    val clientType = "app"

                    val requestBody = """{"refreshToken": "$refreshToken"}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/logout")
                            .header("X-Client-Type", clientType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."))
                        .andExpect(jsonPath("$.data").isEmpty)
                        .andExpect(header().doesNotExist("Set-Cookie"))

                    verify(memberService).logout(refreshToken)
                }
            }
        }
    }
}