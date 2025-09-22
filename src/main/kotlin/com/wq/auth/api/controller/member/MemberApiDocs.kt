package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.response.UserInfoResponseDto
import com.wq.auth.security.annotation.AuthenticatedApi
import com.wq.auth.security.principal.PrincipalDetails
import com.wq.auth.web.common.response.FailResponse
import com.wq.auth.web.common.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "회원", description = "내 정보 조회 등 회원 관련 API")
interface MemberApiDocs {

    @Operation(
        summary = "현재 로그인한 사용자 정보 조회",
        description = "로그인한 사용자의 닉네임과 이메일 정보를 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 로그인 필요",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "회원 정보 조회에 실패했습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    @GetMapping("/api/v1/auth/members/user-info")
    @AuthenticatedApi
    fun getUserInfo(@AuthenticationPrincipal principalDetail: PrincipalDetails): SuccessResponse<UserInfoResponseDto>

}
