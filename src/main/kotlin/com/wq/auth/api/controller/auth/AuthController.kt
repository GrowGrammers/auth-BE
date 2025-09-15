package com.wq.auth.api.controller.auth

import com.wq.auth.api.controller.auth.request.EmailLoginRequestDto
import com.wq.auth.api.controller.auth.request.LogoutRequestDto
import com.wq.auth.api.controller.auth.request.RefreshAccessTokenRequestDto
import com.wq.auth.api.controller.auth.response.LoginResponseDto
import com.wq.auth.api.controller.auth.response.RefreshAccessTokenResponseDto
import com.wq.auth.api.domain.auth.AuthService
import com.wq.auth.api.domain.auth.error.AuthException
import com.wq.auth.api.domain.auth.error.AuthExceptionCode
import com.wq.auth.api.domain.email.AuthEmailService
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
class AuthController(
    private val authService: AuthService,
    private val emailService: AuthEmailService,
    private val jwtProperties: JwtProperties,
) : AuthApiDocs {

    @PostMapping("api/v1/auth/members/email-login")
    @PublicApi
    override fun emailLogin(
        response: HttpServletResponse,
        @RequestHeader("X-Client-Type", required = true) clientType: String,
        @RequestBody req: EmailLoginRequestDto,

        ): SuccessResponse<LoginResponseDto> {
        emailService.verifyCode(req.email, req.verifyCode)
        val (accessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt) = authService.emailLogin(
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

        val currentRefreshToken = when (clientType) {
            "web" -> refreshToken
            "app" -> req?.refreshToken
            else -> null
        }

        if (currentRefreshToken.isNullOrBlank()) {
            throw AuthException(AuthExceptionCode.LOGOUT_FAILED)
        }

        authService.logout(currentRefreshToken)

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
        val (accessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt) = authService.refreshAccessToken(
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

}
