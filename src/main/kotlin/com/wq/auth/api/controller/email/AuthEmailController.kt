package com.wq.auth.api.controller.email

import com.wq.auth.api.controller.email.request.EmailRequestDto
import com.wq.auth.api.controller.email.request.EmailVerifyRequestDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.web.common.response.BaseResponse
import com.wq.auth.web.common.response.Responses
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/auth/email")
class AuthEmailController(
    private val authEmailService: AuthEmailService
) {
    @PostMapping("/request")
    fun requestCode(@RequestBody req: EmailRequestDto): BaseResponse {
        return try {
            authEmailService.sendVerificationCode(req.email)
            Responses.success(message = "해당 이메일로 인증코드가 발송되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }

    @PostMapping("/verify")
    fun verifyCode(@RequestBody req: EmailVerifyRequestDto): BaseResponse {
        return try {
            authEmailService.verifyCode(req.email, req.code)
            Responses.success(message = "인증되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }
}