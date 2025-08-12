package com.wq.demo.email

import com.wq.demo.shared.utils.VerificationCodeGenerator.generateRandomCode
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class AuthEmailService (
    private val emailRepository: AuthEmailRepository,
    private val mailSender: JavaMailSender,
) {
    fun sendVerificationCode(email: String) {
        val code = generateRandomCode();
        emailRepository.save(EmailVerification(email, code))
        sendEmail(email, "인증 코드", "인증 코드는 $code 입니다.")
    }

    fun verifyCode(email: String, code: String): Boolean {
        val savedCode = emailRepository.findByEmail(email)?.code
        return savedCode == code
    }

    fun sendEmail(to: String, subject: String, text: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.setSubject(subject)
        message.setText(text)
        mailSender.send(message)
    }
}