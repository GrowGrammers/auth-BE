package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.request.EmailLoginRequestDto
import com.wq.auth.api.controller.member.request.LogoutRequestDto
import com.wq.auth.api.controller.member.request.RefreshAccessTokenRequestDto
import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.controller.member.response.RefreshAccessTokenResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.security.annotation.AuthenticatedApi
import com.wq.auth.security.annotation.PublicApi
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.principal.PrincipalDetails
import com.wq.auth.web.common.response.Responses
import com.wq.auth.web.common.response.SuccessResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(
    private val memberService: MemberService,
    private val emailService: AuthEmailService,
    private val jwtProperties: JwtProperties,
) : MemberApiDocs {

    @PostMapping("api/v1/auth/members/email-login")
    @PublicApi
    override fun emailLogin(
        response: HttpServletResponse,
        @RequestHeader("X-Client-Type", required = true) clientType: String,
        @RequestBody req: EmailLoginRequestDto,

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
                //.sameSite("None") 배포시
                .sameSite("Lax")
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

    @PostMapping("api/v1/auth/members/logout")
    @AuthenticatedApi
    override fun logout(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse,
        @RequestHeader(name = "X-Client-Type", required = true) clientType: String,
        @AuthenticationPrincipal principalDetail: PrincipalDetails,
        @RequestBody req: LogoutRequestDto?
    ): SuccessResponse<Void?> {

        val currentRefreshToken : String?
        if(clientType == "web") {
            currentRefreshToken = refreshToken
        } else {
            currentRefreshToken = req?.refreshToken
        }
        memberService.logout(currentRefreshToken!!)

        if (clientType == "web") {
            val refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                //.secure(true) 배포시
                .secure(false)
                .path("/")
                .maxAge(0)
                //.sameSite("None") 배포시
                .sameSite("Lax")
                .build()
            response.addHeader("Set-Cookie", refreshCookie.toString())

        }
        //앱
        return Responses.success(message = "로그아웃에 성공했습니다.", data = null)
    }

    @PostMapping("api/v1/auth/members/refresh")
    @PublicApi
    override fun refreshAccessToken(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String,
        @RequestHeader("X-Client-Type") clientType: String,
        response: HttpServletResponse,
        @RequestBody req: RefreshAccessTokenRequestDto?,
    ): SuccessResponse<RefreshAccessTokenResponseDto> {
        val currentRefreshToken : String?
        if(clientType == "web") {
            currentRefreshToken = refreshToken
        } else {
            currentRefreshToken = req?.refreshToken
        }
        val (accessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt) = memberService.refreshAccessToken(
            currentRefreshToken!!, req?.deviceId,
            clientType = clientType
        )

        if (clientType == "web") {
            val refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                //.secure(true) 배포시
                .secure(false)
                .path("/")
                .maxAge(jwtProperties.refreshExp.toSeconds())
                //.sameSite("None") 배포시
                .sameSite("Lax")
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

    @GetMapping("/api/v1/members")
    fun getAll(): SuccessResponse<List<MemberEntity>> =
        Responses.success("회원 목록 조회 성공", memberService.getAll())

    @GetMapping("/api/v1/members/{id}")
    fun getById(@PathVariable id: Long): SuccessResponse<MemberEntity?> =
        Responses.success("회원 조회 성공", memberService.getById(id))

    @PostMapping("/api/v1/members")
    fun create(@RequestBody member: MemberEntity): SuccessResponse<MemberEntity> =
        Responses.success("회원 생성 성공", memberService.create(member))

    @DeleteMapping("/api/v1/members/{id}")
    fun delete(@PathVariable id: Long): SuccessResponse<Void> {
        memberService.delete(id)
        return Responses.success("회원 삭제 성공")
    }

    @PutMapping("/api/v1/members/{id}/nickname")
    fun updateNickname(
        @PathVariable id: Long,
        @RequestBody payload: Map<String, String>
    ): SuccessResponse<MemberEntity?> {
        val newNickname = payload["nickname"] ?: throw IllegalArgumentException("닉네임은 필수입니다")
        return Responses.success("닉네임 변경 성공", memberService.updateNickname(id, newNickname))
    }

}
