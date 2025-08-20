package com.wq.demo.email

import org.springframework.data.jpa.repository.JpaRepository

interface AuthEmailRepository : JpaRepository<EmailVerificationEntity, String> {
    fun findByEmail(email: String): EmailVerificationEntity?
}