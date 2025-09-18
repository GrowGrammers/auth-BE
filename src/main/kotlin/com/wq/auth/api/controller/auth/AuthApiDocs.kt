package com.wq.auth.api.controller.auth

import com.wq.auth.api.controller.auth.request.EmailLoginRequestDto
import com.wq.auth.api.controller.auth.request.LogoutRequestDto
import com.wq.auth.api.controller.auth.request.RefreshAccessTokenRequestDto
import com.wq.auth.api.controller.auth.response.LoginResponseDto
import com.wq.auth.api.controller.auth.response.RefreshAccessTokenResponseDto
import com.wq.auth.security.principal.PrincipalDetails
import com.wq.auth.web.common.response.BaseResponse
import com.wq.auth.web.common.response.FailResponse
import com.wq.auth.web.common.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Tag(name = "회원", description = "로그인, 로그아웃 등 회원 관련 API")
interface AuthApiDocs {

    @Operation(
        summary = "이메일 로그인",
        description = "회원 가입을 한 사용자 라면, 회원 이메일로 로그인하고 회원 가입을 하지 않은 사용자라면 회원가입 후 AccessToken과 RefreshToken을 발급합니다."
    )

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "이메일 인증 실패",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "회원 정보를 저장하는데 실패했습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    fun emailLogin(
        response: HttpServletResponse,
        @RequestHeader("X-Client-Type") clientType: String,
        @RequestBody req: EmailLoginRequestDto
    ): SuccessResponse<LoginResponseDto>

    @Operation(
        summary = "로그아웃",
        description = "RefreshToken을 DB에서 삭제하여 로그아웃합니다."
    )

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "로그아웃에 실패했습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )

    fun logout(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse,
        @RequestHeader(name = "X-Client-Type", required = true) clientType: String,
        @RequestBody req: LogoutRequestDto?
    ): SuccessResponse<Void?>

    @Operation(
        summary = "액세스 토큰 재발급",
        description = "유효한 리프레시 토큰을 이용해 새로운 액세스 토큰과 리프레시 토큰을 발급합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "액세스 토큰 재발급 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = RefreshAccessTokenResponseDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청, 인증 토큰이 없음, Authorization 헤더는 'Bearer <token>' 형식이어야 합니다.",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = BaseResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 토큰, 만료된 토큰, 유효하지 않은 서명, 지원되지 않는 토큰",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = BaseResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "refreshToken 조회 실패",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = BaseResponse::class)
                )]
            )
        ]
    )
     fun refreshAccessToken(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String,
        @RequestHeader("X-Client-Type") clientType: String,
        response: HttpServletResponse,
        @RequestBody req: RefreshAccessTokenRequestDto?,
    ): SuccessResponse<RefreshAccessTokenResponseDto>
}
