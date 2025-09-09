package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.request.EmailLoginRequestDto
import com.wq.auth.api.controller.member.request.LogoutRequestDto
import com.wq.auth.api.controller.member.request.RefreshAccessTokenRequestDto
import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.controller.member.response.RefreshAccessTokenResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.web.common.response.Responses
import com.wq.auth.web.common.response.SuccessResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(
    private val memberService: MemberService,
    private val emailService: AuthEmailService,
    private val jwtProperties: JwtProperties,
    private val jwtProvider: JwtProvider,
) : MemberApiDocs {

    @PostMapping("api/v1/auth/members/email-login")
    override fun emailLogin(
        response: HttpServletResponse,
        @RequestHeader("X-Client-Type", required = true) clientType: String,
        @RequestBody req: EmailLoginRequestDto
    ): SuccessResponse<LoginResponseDto> {
        emailService.verifyCode(req.email, req.verifyCode)
        val (accessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt) = memberService.emailLogin(
            req.email,
            deviceId = req.deviceId,
            clientType = clientType
        )

        if (clientType == "web") {
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

            val resp = LoginResponseDto.forWeb(
                accessToken = accessToken,
                expiredAt = accessTokenExpiredAt
            )

            return Responses.success(message = "로그인에 성공했습니다.", data = resp)
        }

        val resp = LoginResponseDto.forApp(
            refreshToken = newRefreshToken,
            refreshExpiredAt = refreshTokenExpiredAt
        )
        return Responses.success(message = "로그인에 성공했습니다.", data = resp)
    }

    //헤더에 액세스토큰 추가
    @PostMapping("api/v1/auth/members/logout")
    override fun logout(
        @CookieValue(name = "refreshToken", required = true) refreshToken: String?,
        response: HttpServletResponse,
        @RequestHeader(name = "AccessToken", required = false) accessToken: String?,
        @RequestHeader(name = "X-Client-Type", required = true) clientType: String,
        @RequestBody req: LogoutRequestDto
    ): SuccessResponse<Void?> {

        val currentRefreshToken : String?
        if(clientType == "web") {
            currentRefreshToken = refreshToken
        } else {
            currentRefreshToken = req.refreshToken
        }
        memberService.logout(currentRefreshToken!!)

        if (clientType == "web") {
            jwtProvider.validateOrThrow(accessToken!!)

            val refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                //.secure(true) 배포시
                .secure(false)
                .path("/")
                .maxAge(0)
                //.sameSite("Strict") 배포시
                .sameSite("None")
                .build()
            response.addHeader("Set-Cookie", refreshCookie.toString())

        }
        //앱
        return Responses.success(message = "로그아웃에 성공했습니다.", data = null)
    }

    @PostMapping("api/v1/auth/members/refresh")
    override fun refreshAccessToken(
        @CookieValue(name = "refreshToken", required = true) refreshToken: String,
        @RequestHeader("X-Client-Type") clientType: String,
        response: HttpServletResponse,
        @RequestBody req: RefreshAccessTokenRequestDto
    ): SuccessResponse<RefreshAccessTokenResponseDto> {
        val currentRefreshToken : String?
        if(clientType == "web") {
            currentRefreshToken = refreshToken
        } else {
            currentRefreshToken = req.refreshToken
        }
        val (accessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt) = memberService.refreshAccessToken(
            currentRefreshToken!!, req.deviceId,
            clientType = clientType
        )

        if (clientType == "web") {
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

            val resp = RefreshAccessTokenResponseDto.forWeb(
                accessToken = accessToken,
                expiredAt = accessTokenExpiredAt
            )
            return Responses.success(message = "AccessToken 재발급에 성공했습니다.", data = resp)
        }
        //앱
        //TODO refreshToken만 쓰도록 수정
        val resp = RefreshAccessTokenResponseDto.forApp(
            refreshToken = newRefreshToken,
            refreshExpiredAt = refreshTokenExpiredAt
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
