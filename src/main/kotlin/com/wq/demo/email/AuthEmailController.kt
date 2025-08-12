package com.wq.demo.email

import com.wq.demo.web.common.BaseResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/auth/email")
class AuthEmailController (
    private val authEmailService: AuthEmailService
) {
    @PostMapping("/request")
    fun requestCode(@RequestBody req: EmailRequestDto): BaseResponse<Unit> {
        println("requestCode")
        authEmailService.sendVerificationCode(req.email)
        return BaseResponse(success = true, message = "해당 이메일로 인증코드가 발송되었습니다.", data = null)
    }

    @PostMapping("/verify")
    fun verifyCode(@RequestBody req: EmailVerifyRequestDto): BaseResponse<Unit> {
        val verified = authEmailService.verifyCode(req.email, req.code)
        return if (verified) BaseResponse(success = true, message = "인증되었습니다." , data = null)
        else BaseResponse(success = false, message = "인증에 실패했습니다.", data = null)
    }
}