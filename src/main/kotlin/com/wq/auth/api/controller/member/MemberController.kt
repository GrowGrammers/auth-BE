package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.request.EmailLoginRequestDto
import com.wq.auth.api.controller.member.request.LogoutRequestDto
import com.wq.auth.api.controller.member.request.RefreshAccessTokenRequestDto
import com.wq.auth.api.controller.member.response.RefreshAccessTokenResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.error.JwtException
import com.wq.auth.shared.error.ApiException
import com.wq.auth.shared.error.CommonExceptionCode
import com.wq.auth.web.common.response.BaseResponse
import com.wq.auth.web.common.response.Responses
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(
    private val memberService: MemberService,
    private val emailService: AuthEmailService,
    private val jwtProperties: JwtProperties
) : MemberApiDocs {

    @PostMapping("api/v1/auth/members/email-login")
    override fun emailLogin(@RequestBody req: EmailLoginRequestDto): BaseResponse {
        return try {
            emailService.verifyCode(req.email, req.verifyCode)
            val resp = memberService.emailLogin(req.email)
            Responses.success(message = "로그인에 성공했습니다.", data = resp)
        } catch (e: ApiException) {
            val code = when (e) {
                is MemberException -> e.memberCode
                is EmailException -> e.emailCode
                is JwtException -> e.jwtCode
                else -> null
            }
            Responses.fail(code ?: CommonExceptionCode.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("api/v1/auth/members/logout")
    override fun logout(@RequestBody req: LogoutRequestDto): BaseResponse {
        return try {
            memberService.logout(req.refreshToken)
            Responses.success(message = "로그아웃에 성공했습니다.", data = null)
        } catch (e: MemberException) {
            Responses.fail(e.memberCode)
        }
    }

    @PostMapping("api/v1/auth/members/refresh")
    override fun refreshAccessToken(
        @CookieValue(name = "refreshToken", required = true) refreshToken: String,
        response: HttpServletResponse,
        @RequestBody req: RefreshAccessTokenRequestDto): BaseResponse {
        val (accessToken, newRefreshToken, accessTokenExpiredAt) = memberService.refreshAccessToken(req.refreshToken)

        val refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
            .httpOnly(true)
            //.secure(true) 배포시
            .secure(false)
            .path("/")
            .maxAge(jwtProperties.refreshExp.toSeconds())
            //.sameSite("Strict") 배포시
            .sameSite("None")
            .build()
        response.addHeader("Set-Cookie", refreshCookie.toString())

        val resp = RefreshAccessTokenResponseDto(
            accessToken = accessToken,
            expiredAt = accessTokenExpiredAt
        )
        return Responses.success(message = "AccessToken 재발급에 성공했습니다.", data = resp)
    }

    @GetMapping("/members")
    fun getAll(): List<MemberEntity> = memberService.getAll()

    @GetMapping("/members/{id}")
    fun getById(@PathVariable id: Long): MemberEntity? = memberService.getById(id)

    @PostMapping("/members")
    fun create(@RequestBody member: MemberEntity): MemberEntity = memberService.create(member)

    @DeleteMapping("/members/{id}")
    fun delete(@PathVariable id: Long) = memberService.delete(id)

    @PutMapping("/members/{id}/nickname")
    fun updateNickname(
        @PathVariable id: Long,
        @RequestBody payload: Map<String, String>
    ): MemberEntity? {
        val newNickname = payload["nickname"] ?: return null
        return memberService.updateNickname(id, newNickname)
    }

}
