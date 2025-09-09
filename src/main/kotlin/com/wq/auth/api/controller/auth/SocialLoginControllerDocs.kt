package com.wq.auth.api.controller.auth

import com.wq.auth.api.controller.auth.request.SocialLoginRequestDto
import com.wq.auth.web.common.response.FailResponse
import com.wq.auth.web.common.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

/**
 * 소셜 로그인 API 문서화 인터페이스
 * 
 * 소셜 로그인 관련 API의 Swagger 문서를 정의합니다.
 */
@Tag(name = "소셜 로그인", description = "Google, 카카오, 네이버 등 소셜 제공자를 통한 로그인 API")
interface SocialLoginControllerDocs {

    @Operation(
        summary = "범용 소셜 로그인",
        description = """
            프론트엔드에서 소셜 제공자로부터 받은 인가 코드를 사용하여 사용자 정보를 조회하고 JWT 토큰을 발급합니다.
            
            **redirectUri 파라미터:**
            - 선택사항: 미제공시 properties에 설정된 기본값 사용
            - 각 소셜 제공자의 OAuth2 설정에서 승인된 리다이렉트 URI와 일치해야 함
            - 프론트엔드 환경별로 다른 URI 사용 가능 (개발/스테이징/프로덕션)
            
            **토큰 반환 방식:**
            - Access Token: Authorization 헤더에 Bearer 방식으로 반환
            - Refresh Token: HttpOnly 쿠키로 설정 (XSS 공격 방지)
            
            **지원 소셜 제공자:**
            - GOOGLE: Google OAuth2
            - KAKAO: 카카오 OAuth2 (향후 지원 예정)
            - NAVER: 네이버 OAuth2 (향후 지원 예정)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공 - Authorization 헤더에 Bearer 토큰, HttpOnly 쿠키에 리프레시 토큰 설정",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (필수 필드 누락, 잘못된 형식 등)",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인가 코드가 유효하지 않거나 만료됨",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "소셜 제공자 API 호출 실패 또는 서버 내부 오류",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequestDto,
        response: HttpServletResponse
    ): SuccessResponse<Void>

    @Operation(
        summary = "Google 소셜 로그인",
        description = """
            Google 전용 편의 메서드로, providerType을 별도로 지정하지 않아도 됩니다.
            
            **사용 방법:**
            1. 프론트엔드에서 Google OAuth2 인증 URL로 사용자를 리다이렉트
            2. 사용자가 Google에서 인증 완료 후 받은 인가 코드(code)를 이 API로 전송
            3. 백엔드에서 Google API를 통해 사용자 정보 조회 후 JWT 토큰 발급
            
            **redirectUri 파라미터:**
            - 선택사항: 미제공시 properties에 설정된 기본값 사용
            - Google OAuth2 설정의 승인된 리다이렉트 URI와 일치해야 함
            - 프론트엔드 환경별로 다른 URI 사용 가능
            
            **토큰 반환 방식:**
            - Access Token: Authorization 헤더에 Bearer 방식으로 반환
            - Refresh Token: HttpOnly 쿠키로 설정 (XSS 공격 방지)
            
            **쿠키 설정:**
            - HttpOnly: JavaScript 접근 불가 (XSS 방지)
            - Path=/: 모든 경로에서 사용 가능
            - Max-Age=1209600: 14일 만료
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Google 로그인 성공 - Authorization 헤더에 Bearer 토큰, HttpOnly 쿠키에 리프레시 토큰 설정",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "인가 코드(code) 파라미터가 누락되거나 잘못된 형식",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Google 인가 코드가 유효하지 않거나 만료됨",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Google API 호출 실패 또는 서버 내부 오류",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    fun googleLogin(
        @Valid @RequestBody request: com.wq.auth.api.controller.auth.request.GoogleSocialLoginRequestDto,
        response: HttpServletResponse
    ): SuccessResponse<Void>
}
