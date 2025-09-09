package com.wq.auth.api.controller.auth

import com.wq.auth.api.controller.auth.request.GoogleSocialLoginRequestDto
import com.wq.auth.api.controller.auth.request.SocialLoginRequestDto
import com.wq.auth.api.controller.auth.request.toDomain
import com.wq.auth.api.domain.member.entity.ProviderType
import com.wq.auth.domain.auth.SocialLoginService
import com.wq.auth.security.annotation.PublicApi
import com.wq.auth.web.common.response.Responses
import com.wq.auth.web.common.response.SuccessResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * 소셜 로그인 컨트롤러
 * 
 * 소셜 로그인 관련 API 엔드포인트를 제공합니다.
 * - Google, 카카오, 네이버 등 소셜 제공자를 통한 로그인 처리
 * - 인가 코드를 받아 JWT 토큰 발급
 */
@RestController
class SocialLoginController(
    private val socialLoginService: SocialLoginService,

    @Value("\${app.cookie.secure:false}")
    private val cookieSecure: Boolean,

    @Value("\${app.cookie.same-site:Strict}")
    private val cookieSameSite: String
) : SocialLoginControllerDocs {

    /**
     * 범용 소셜 로그인 처리
     * 
     * 프론트엔드에서 소셜 제공자로부터 받은 인가 코드를 사용하여
     * 사용자 정보를 조회하고 JWT 토큰을 발급합니다.
     * 
     * @param request 소셜 로그인 요청 (인가 코드, 제공자 타입 등)
     * @return JWT 토큰과 사용자 정보가 포함된 응답
     */
    @PublicApi("소셜 로그인")
    @PostMapping("/api/v1/auth/social/login")
    override fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequestDto,
        response: HttpServletResponse
    ): SuccessResponse<Void> {
        val loginResult = socialLoginService.processSocialLogin(request.toDomain())
        
        // RefreshToken을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(response, loginResult.refreshToken)
        
        // Authorization 헤더에 AccessToken 설정
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.accessToken}")
        
        return Responses.success("소셜 로그인이 완료되었습니다")
    }

    /**
     * Google 소셜 로그인 (편의 메서드)
     * 
     * Google 전용 엔드포인트로, providerType을 별도로 지정하지 않아도 됩니다.
     * 
     * @param authorizationCode Google 인가 코드
     * @param redirectUri 리다이렉트 URI (선택사항)
     * @return JWT 토큰과 사용자 정보가 포함된 응답
     */
    @PublicApi("Google 소셜 로그인")
    @PostMapping("/api/v1/auth/google/login")
    override fun googleLogin(
        @Valid @RequestBody request: GoogleSocialLoginRequestDto,
        response: HttpServletResponse
    ): SuccessResponse<Void> {
        val serviceRequest = SocialLoginRequestDto(
            authCode = request.authCode,
            codeVerifier = request.codeVerifier,
            providerType = ProviderType.GOOGLE,
        )
        
        val loginResult = socialLoginService.processSocialLogin(serviceRequest.toDomain())
        
        // RefreshToken을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(response, loginResult.refreshToken)
        
        // Authorization 헤더에 AccessToken 설정
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.accessToken}")
        
        return Responses.success("Google 로그인이 완료되었습니다")
    }
    
    /**
     * RefreshToken을 HttpOnly 쿠키로 설정합니다.
     * 
     * Spring Boot 3.x의 ResponseCookie를 사용하여 현대적이고 안전한 쿠키를 생성합니다.
     * - HttpOnly: JavaScript 접근 불가 (XSS 방지)
     * - Secure: HTTPS에서만 전송 (프로덕션 환경)
     * - SameSite: CSRF 공격 방지
     * - MaxAge: 14일 (리프레시 토큰 만료 시간과 동일)
     * 
     * @param response HTTP 응답 객체
     * @param refreshToken 리프레시 토큰
     */
    private fun setRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)                              // XSS 공격 방지
            .secure(cookieSecure)                                  // 환경별 설정 (개발: false, 프로덕션: true)
            .path("/")                                      // 모든 경로에서 쿠키 사용 가능
            .maxAge(Duration.ofDays(14))          // 14일 만료
            .sameSite(cookieSameSite)                              // CSRF 공격 방지 (Strict/Lax/None)
            .build()
            
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
