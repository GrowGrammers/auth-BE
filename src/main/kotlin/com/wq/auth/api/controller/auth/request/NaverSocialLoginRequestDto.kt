package com.wq.auth.api.controller.auth.request

import com.wq.auth.domain.auth.request.NaverSocialLoginRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Naver 소셜 로그인 요청 바디")
data class NaverSocialLoginRequestDto(
    @field:NotBlank(message = "authCode는 필수입니다")
    @field:Schema(description = "Naver OAuth2에서 받은 인가 코드", example = "4/0AX4XfWh...AbCdEfGhIj")
    val authCode: String,

    @field:NotBlank(message = "state는 필수입니다")
    @field:Schema(description = "CSRF 방지용 상태 값", example = "random_state_string_12345")
    val state: String,

    @field:NotBlank(message = "grantType은 필수입니다")
    @field:Schema(description = "OAuth2 그랜트 타입", example = "authorization_code", allowableValues = ["authorization_code"])
    val grantType: String = "authorization_code",

    @field:Schema(
        description = "리다이렉트 URI (선택사항, 미제공시 properties에 설정된 기본값 사용)", 
        example = "http://localhost:3000/auth/callback"
    )
    val redirectUri: String? = null
)

fun NaverSocialLoginRequestDto.toDomain(): NaverSocialLoginRequest =
    NaverSocialLoginRequest(authCode, state, grantType, redirectUri)