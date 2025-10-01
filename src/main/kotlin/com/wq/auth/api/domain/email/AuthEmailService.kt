package com.wq.auth.api.domain.email

import com.wq.auth.api.domain.email.entity.EmailVerificationEntity
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.api.domain.email.error.EmailExceptionCode
import com.wq.auth.shared.utils.VerificationCodeGenerator.generateRandomCode
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.*
import javax.naming.directory.InitialDirContext

@Service
class AuthEmailService(
    private val emailRepository: AuthEmailRepository,
    private val mailSender: JavaMailSender,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendVerificationCode(email: String) {
        log.info("sendVerificationCode() called for email={}", email)
        validateEmailFormat(email)
        validateDomain(email)

        val code = generateRandomCode()
        log.debug("Generated verification code={} for email={}", code, email)

        //TODO 인증번호 5분 제한, 제거시 soft delete
        sendEmail(email, "인증 코드", "인증 코드는 $code 입니다.")
        emailRepository.save(EmailVerificationEntity(email, code))
        log.info("Verification code saved for email={}", email)
    }

    fun verifyCode(email: String, code: String) {
        log.info("verifyCode() called for email={} with code={}", email, code)
        val savedCode = emailRepository.findByEmail(email)?.code
        log.debug("Saved code from DB={} for email={}", savedCode, email)

        if (savedCode == null || savedCode != code) {
            log.warn("Email verification failed for email={}", email)
            throw EmailException(EmailExceptionCode.EMAIL_VERIFICATION_FAILED)
        }
        log.info("Email verification success for email={}", email)
    }

    fun sendEmail(to: String, subject: String, text: String) {
        log.info("sendEmail() called → to={}, subject={}", to, subject)
        val message = MailMessage.of(to, subject, text)
        try {
            mailSender.send(message)
            log.info("Email successfully sent to={}", to)
        } catch (e: Exception) {
            log.error("Email sending failed to={}, subject={}, cause={}", to, subject, e.message, e)
            throw EmailException(EmailExceptionCode.EMAIL_NOT_SENDED, e)
        }
    }

    fun validateEmailFormat(email: String) {
        try {
            InternetAddress(email).validate()
            log.debug("Email format valid: {}", email)
        } catch (e: Exception) {
            log.warn("Invalid email format: {}", email)
            throw EmailException(EmailExceptionCode.INVALID_EMAIL_FORMAT)
        }
    }

    private fun validateDomain(email: String) {
        val domain = email.substringAfter("@")
        log.debug("Validating domain for email={}, domain={}", email, domain)

        val env = Hashtable<String, String>()
        env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
        try {
            val ictx = InitialDirContext(env)
            val attrs = ictx.getAttributes(domain, arrayOf("MX"))
            val mxAttr = attrs.get("MX")
            if (mxAttr == null || mxAttr.size() == 0) {
                log.warn("Domain found but no MX record: {}", domain)
                throw EmailException(EmailExceptionCode.EMAIL_SERVER_NOT_FOUND)
            }
            log.debug("Domain {} has MX records", domain)
        } catch (e: javax.naming.NamingException) {
            log.error("Domain not found: {}, cause={}", domain, e.message)
            throw EmailException(EmailExceptionCode.DOMAIN_NOT_FOUND, e)
        }
    }
}
