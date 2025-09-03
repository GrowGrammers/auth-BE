package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.request.EmailLoginRequestDto
import com.wq.auth.web.common.response.BaseResponse
import com.wq.auth.web.common.response.FailResponse
import com.wq.auth.web.common.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "회원", description = "로그인, 로그아웃 등 회원 관련 API")
interface MemberApiDocs {

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
    fun emailLogin(@RequestBody req: EmailLoginRequestDto): BaseResponse
}
