package com.wq.demo.email

import com.wq.demo.shared.utils.VerificationCodeGenerator.generateRandomCode
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

    fun verifyCode(email: String, code: String): Boolean {
        val savedCode = emailRepository.findByEmail(email)?.code
        return savedCode == code
    }

    fun sendEmail(to: String, subject: String, text: String) {
        val message = MailMessage.of(to, subject, text)
        try {
            mailSender.send(message)
        } catch (e: Exception) {
            // SMTP 단계에서 도메인 불가 / 연결 불가 등은 여기서 잡힘
            throw IllegalStateException("메일 전송 실패: ${e.message}", e)
        }
    }

    private fun validateEmailFormat(email: String) {
        InternetAddress(email).validate()
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
                throw IllegalArgumentException("존재하지 않는 도메인입니다: $domain")
            }
            //도메인 자체가 없는 경우
        } catch (e: javax.naming.NamingException) {
            throw IllegalArgumentException("존재하지 않는 도메인입니다: $domain", e)
        }
    }
}