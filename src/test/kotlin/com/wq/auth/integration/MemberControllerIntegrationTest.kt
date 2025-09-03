package com.wq.auth.integration

import com.wq.auth.api.controller.member.MemberController
import com.wq.auth.api.domain.member.MemberService
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

    init {
        describe("POST /api/v1/auth/members/refresh") {

            context("유효한 요청이 주어졌을 때") {
                it("성공 응답과 새로운 토큰을 반환해야 한다") {
                    // given
                    val refreshToken = "valid-refresh-token"
                    val newAccessToken = "new-access-token"
                    val newRefreshToken = "new-refresh-token"
                    val expiredAt = System.currentTimeMillis() + 3600000

                    val tokenResult = MemberService.TokenResult(newAccessToken, newRefreshToken, expiredAt)
                    given(memberService.refreshAccessToken(refreshToken)).willReturn(tokenResult)

                    val requestBody = """{"refreshToken": "$refreshToken"}"""

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .cookie(Cookie("refreshToken", refreshToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("AccessToken 재발급에 성공했습니다."))
                        .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
                        .andExpect(jsonPath("$.data.expiredAt").value(expiredAt))
                        .andExpect(
                            header().string(
                                "Set-Cookie",
                                Matchers.containsString("refreshToken=$newRefreshToken")
                            )
                        )
                        .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")))
                        .andExpect(header().string("Set-Cookie", Matchers.containsString("SameSite=None")))

                    verify(memberService).refreshAccessToken(refreshToken)
                }
            }

            context("서비스에서 예외가 발생했을 때") {
                it("적절한 에러 응답을 반환해야 한다") {
                    // given
                    val refreshToken = "invalid-refresh-token"
                    val requestBody = """{"refreshToken": "$refreshToken"}"""

                    given(memberService.refreshAccessToken(refreshToken))
                        .willThrow(JwtException(JwtExceptionCode.MALFORMED))

                    // when & then
                    mockMvc.perform(
                        post("/api/v1/auth/members/refresh")
                            .cookie(Cookie("refreshToken", refreshToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    ).andExpect(status().isUnauthorized)

                    verify(memberService).refreshAccessToken(refreshToken)
                }
            }
        }
    }
}
