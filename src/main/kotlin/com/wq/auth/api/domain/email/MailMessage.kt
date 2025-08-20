package com.wq.auth.api.domain.email

import org.springframework.mail.SimpleMailMessage

class MailMessage private constructor() {
    companion object {
        fun of(to: String, subject: String, text: String): SimpleMailMessage =
            SimpleMailMessage().apply {
                setTo(to)
                setSubject(subject)
                setText(text)
            }
    }
}