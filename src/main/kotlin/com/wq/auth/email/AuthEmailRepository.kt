package com.wq.auth.email

import org.springframework.data.jpa.repository.JpaRepository

interface AuthEmailRepository : JpaRepository<EmailVerificationEntity, String> {
    fun findByEmail(email: String): EmailVerificationEntity?
}