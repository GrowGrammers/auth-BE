package com.wq.demo.unit

import com.wq.demo.email.AuthEmailRepository
import com.wq.demo.email.AuthEmailService
import com.wq.demo.email.EmailVerificationEntity
import com.wq.demo.email.error.EmailException
import com.wq.demo.email.error.EmailExceptionCode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import jakarta.mail.internet.AddressException
import org.mockito.kotlin.*
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class AuthEmailServiceTest : StringSpec({

    val emailRepository = mock<AuthEmailRepository>()
    val mailSender = mock<JavaMailSender>()
    val authEmailService = AuthEmailService(emailRepository, mailSender)

    "정상 이메일 전송" {
        val validEmail = "test@gmail.com"
        authEmailService.sendVerificationCode(validEmail)
    }

    "잘못된 이메일 형식 INVALID_EMAIL_FORMAT Exception 발생" {
        val invalidEmail = "invalid-email"
        val exception = shouldThrow<EmailException> {
            authEmailService.sendVerificationCode(invalidEmail)
        }
        exception.emailCode shouldBe EmailExceptionCode.INVALID_EMAIL_FORMAT
    }

    "존재하지 않는 도메인 검증 시 DOMAIN_NOT_FOUND EmailException 발생" {
        val nonExistentDomainEmail = "user@no-such-domain.zzz"
        val exception = shouldThrow<EmailException> {
            authEmailService.sendVerificationCode(nonExistentDomainEmail)
        }
        exception.emailCode shouldBe EmailExceptionCode.DOMAIN_NOT_FOUND
    }

    "메일 전송 실패 시 EMAIL_NOT_SENDED EmailException 발생" {
        val email = "test@gmail.com"
        val code = "123456"

        //강제 예외 발생
        doThrow(RuntimeException("SMTP 연결 실패"))
            .whenever(mailSender)
            .send(any<SimpleMailMessage>())

        val exception = shouldThrow<EmailException> {
            authEmailService.sendEmail(email, "인증코드", "인증 코드는 $code 입니다.")
        }
        exception.emailCode shouldBe EmailExceptionCode.EMAIL_NOT_SENDED
    }
    "인증 코드 검증 성공" {
        val email = "user@gmail.com"
        val code = "123456"
        val entity = EmailVerificationEntity(email, code)

        // repository 동작 모킹
        whenever(emailRepository.findByEmail(email)).thenReturn(entity)

        authEmailService.verifyCode(email, code)
    }

    "잘못된 인증 코드 검증 시 EMAIL_VERIFICATION_FAILED EmailException 발생" {
        val email = "user@gmail.com"
        val code = "123456"
        val entity = EmailVerificationEntity(email, code)

        // repository 동작 모킹
        whenever(emailRepository.findByEmail(email)).thenReturn(entity)

        val exception = shouldThrow<EmailException> {
            authEmailService.verifyCode(email, "wrong-code")
        }
        exception.emailCode shouldBe EmailExceptionCode.EMAIL_VERIFICATION_FAILED
    }

})
