package com.wq.demo.unit

import com.wq.demo.email.AuthEmailRepository
import com.wq.demo.email.AuthEmailService
import com.wq.demo.email.EmailVerificationEntity
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

    "잘못된 이메일 형식은 AddressException 발생" {
        val invalidEmail = "invalid-email"
        shouldThrow<AddressException> {
            authEmailService.sendVerificationCode(invalidEmail)
        }
    }

    "존재하지 않는 도메인은 IllegalArgumentException 발생" {
        val nonExistentDomainEmail = "user@no-such-domain.zzz"
        shouldThrow<IllegalArgumentException> {
            authEmailService.sendVerificationCode(nonExistentDomainEmail)
        }
    }

    "메일 전송 실패 시 IllegalStateException 발생" {
        val email = "test@gmail.com"
        val code = "123456"

        //강제 예외 발생
        doThrow(RuntimeException("SMTP 연결 실패"))
            .whenever(mailSender)
            .send(any<SimpleMailMessage>())

        shouldThrow<IllegalStateException> {
            authEmailService.sendEmail(email, "제목", "본문")
        }
    }

    "인증 코드 검증" {
        val email = "user@gmail.com"
        val code = "123456"
        val entity = EmailVerificationEntity(email, code)

        // repository 동작 모킹
        whenever(emailRepository.findByEmail(email)).thenReturn(entity)

        authEmailService.verifyCode(email, code) shouldBe true
        authEmailService.verifyCode(email, "wrong-code") shouldBe false
    }

})
