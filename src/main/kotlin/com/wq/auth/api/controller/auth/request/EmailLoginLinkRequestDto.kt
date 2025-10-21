package com.wq.auth.api.controller.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import com.wq.auth.api.domain.auth.request.EmailLoginLinkRequest

@Schema(description = "이메일 로그인 연동 요청")
data class EmailLoginLinkRequestDto(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    @Schema(description = "연동할 이메일 주소", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "인증 코드는 필수입니다.")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    @Schema(description = "6자리 인증 코드", example = "123456")
    val verifyCode: String,
) {
    fun toDomain() = EmailLoginLinkRequest(
        email = email.lowercase(),
        verifyCode = verifyCode
    )
}