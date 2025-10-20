package com.wq.auth.api.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Google 소셜 로그인 요청 바디")
data class GoogleSocialLinkRequestDto(
    @field:NotBlank(message = "authCode는 필수입니다")
    @field:Schema(description = "Google OAuth2에서 받은 인가 코드", example = "4/0AX4XfWh...AbCdEfGhIj")
    val authCode: String,

    @field:NotBlank(message = "codeVerifier는 필수입니다")
    @field:Schema(description = "PKCE 검증용 코드 검증자", example = "NgAfIySigI...IVxKxbmrpg")
    val codeVerifier: String,

    @field:Schema(
        description = "리다이렉트 URI (선택사항, 미제공시 properties에 설정된 기본값 사용)", 
        example = "http://localhost:3000/auth/callback"
    )
    val redirectUri: String? = null
)
