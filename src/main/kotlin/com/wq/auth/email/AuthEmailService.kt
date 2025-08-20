package com.wq.auth.email

import com.wq.auth.email.error.*
import com.wq.auth.shared.utils.VerificationCodeGenerator.generateRandomCode
import jakarta.mail.internet.InternetAddress
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.*
import javax.naming.directory.InitialDirContext

@Service
class AuthEmailService(
    private val emailRepository: AuthEmailRepository,
    private val mailSender: JavaMailSender,
) {
    fun sendVerificationCode(email: String) {
        validateEmailFormat(email)
        validateDomain(email)

        val code = generateRandomCode()
        sendEmail(email, "인증 코드", "인증 코드는 $code 입니다.")
        emailRepository.save(EmailVerificationEntity(email, code))
    }

    fun verifyCode(email: String, code: String) {
        val savedCode = emailRepository.findByEmail(email)?.code

        if (savedCode == null || savedCode != code) {
            throw EmailException(EmailExceptionCode.EMAIL_VERIFICATION_FAILED)
        }
    }

    fun sendEmail(to: String, subject: String, text: String) {
        val message = MailMessage.of(to, subject, text)
        try {
            mailSender.send(message)
        } catch (e: Exception) {
            throw EmailException(EmailExceptionCode.EMAIL_NOT_SENDED, e)
        }
    }

    private fun validateEmailFormat(email: String) {
        try {
            InternetAddress(email).validate()
        } catch (e: Exception) {
            // 유효성 검사 실패 시 EmailException으로 변환
            throw EmailException(EmailExceptionCode.INVALID_EMAIL_FORMAT)
        }
    }

    private fun validateDomain(email: String) {
        val domain = email.substringAfter("@")
        //DNS 조회시 사용하는 객체
        val env = Hashtable<String, String>()
        env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
        try {
            val ictx = InitialDirContext(env)
            val attrs = ictx.getAttributes(domain, arrayOf("MX"))
            val mxAttr = attrs.get("MX")
            //도메인은 존재하지만 MX 레코드가 없는 경우
            if (mxAttr == null || mxAttr.size() == 0) {
                throw EmailException(EmailExceptionCode.EMAIL_SERVER_NOT_FOUND)
            }
            //도메인 자체가 없는 경우
        } catch (e: javax.naming.NamingException) {
            throw EmailException(EmailExceptionCode.DOMAIN_NOT_FOUND,e)
        }
    }
}