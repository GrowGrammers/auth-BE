package com.wq.auth.api.controller.email

import com.wq.auth.api.controller.email.request.EmailRequestDto
import com.wq.auth.api.controller.email.request.EmailVerifyRequestDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.web.common.response.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
class AuthEmailController(
    private val authEmailService: AuthEmailService
) {
    @Operation(
        summary = "이메일 인증 코드 요청",
        description = "사용자의 이메일로 인증 코드를 발송합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "인증코드 발송 성공",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "올바르지 않은 이메일 형식입니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "해당 도메인에 메일을 보낼 수 없습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "존재하지 않는 도메인입니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "이메일 인증코드 전송에 실패했습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    @PostMapping("api/v1/auth/email/request")
    fun requestCode(@RequestBody req: EmailRequestDto): BaseResponse {
        return try {
            authEmailService.sendVerificationCode(req.email)
            Responses.success(message = "해당 이메일로 인증코드가 발송되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }


    @Operation(
        summary = "이메일 인증 코드 검증",
        description = "사용자가 받은 인증 코드를 검증합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "인증 성공",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "이메일 인증코드가 일치하지 않습니다.",
                content = [Content(schema = Schema(implementation = FailResponse::class))]
            )
        ]
    )
    @PostMapping("api/v1/auth/email/verify")
    fun verifyCode(@RequestBody req: EmailVerifyRequestDto): BaseResponse {
        return try {
            authEmailService.verifyCode(req.email, req.code)
            Responses.success(message = "인증되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }
}