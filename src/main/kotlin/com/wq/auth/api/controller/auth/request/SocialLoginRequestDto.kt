package com.wq.auth.api.controller.auth.request

import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.domain.auth.request.SocialLoginRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 소셜 로그인 요청 DTO
 *
 * 프론트엔드에서 소셜 로그인 인가 코드를 백엔드로 전송할 때 사용합니다.
 * PKCE(Proof Key for Code Exchange) 방식을 지원합니다.
 *
 * @param authCode 소셜 제공자로부터 받은 인가 코드 (필수)
 * @param codeVerifier PKCE 검증을 위한 코드 검증자 (필수)
 * @param providerType 소셜 로그인 제공자 타입 (GOOGLE, KAKAO, NAVER 등)
 * @param redirectUri 리다이렉트 URI (선택사항, 미제공시 properties 기본값 사용)
 */
@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequestDto(

    @field:NotBlank(message = "인가 코드는 필수입니다")
    @field:Schema(description = "소셜 제공자로부터 받은 인가 코드", example = "4/0AX4XfWh...AbCdEfGhIj")
    val authCode: String,

    @field:NotBlank(message = "codeVerifier는 필수입니다")
    @field:Schema(
        description = "PKCE 검증을 위한 코드 검증자 (필수)", 
        example = "NgAfIySigI...IVxKxbmrpg"
    )
    val codeVerifier: String,

    @field:NotNull(message = "제공자 타입은 필수입니다")
    @field:Schema(description = "소셜 로그인 제공자 타입", example = "GOOGLE")
    val providerType: ProviderType,

    @field:Schema(
        description = "리다이렉트 URI (선택사항, 미제공시 properties에 설정된 기본값 사용)", 
        example = "http://localhost:3000/auth/callback"
    )
    val redirectUri: String? = null
)

fun SocialLoginRequestDto.toDomain(): SocialLoginRequest =
    SocialLoginRequest(authCode, codeVerifier, providerType, redirectUri)