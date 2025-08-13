package com.wq.demo.email

import org.springframework.mail.SimpleMailMessage

class MailMessage private constructor(
    val to: String,
    val subject: String,
    val text: String
) {
    companion object {
        fun of(to: String, subject: String, text: String): SimpleMailMessage {
            return SimpleMailMessage().apply {
                setTo(to)
                setSubject(subject)
                setText(text)
            }
        }
    }
}