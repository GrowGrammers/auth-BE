package com.wq.auth.api.controller.email

import com.wq.auth.api.controller.email.request.EmailRequestDto
import com.wq.auth.api.controller.email.request.EmailVerifyRequestDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.web.common.response.*
import org.springframework.web.bind.annotation.*

@RestController
class AuthEmailController(
    private val authEmailService: AuthEmailService
) : AuthEmailApiDocs {

    @PostMapping("api/v1/auth/email/request")
    override fun requestCode(@RequestBody req: EmailRequestDto): BaseResponse {
        return try {
            authEmailService.sendVerificationCode(req.email)
            Responses.success(message = "해당 이메일로 인증코드가 발송되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }

    @PostMapping("api/v1/auth/email/verify")
    override fun verifyCode(@RequestBody req: EmailVerifyRequestDto): BaseResponse {
        return try {
            authEmailService.verifyCode(req.email, req.verifyCode)
            Responses.success(message = "인증되었습니다.", data = null)
        } catch (e: EmailException) {
            Responses.fail(e.emailCode)
        }
    }
}