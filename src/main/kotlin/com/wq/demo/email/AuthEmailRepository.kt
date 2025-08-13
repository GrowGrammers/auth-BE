package com.wq.demo.email

import org.springframework.data.jpa.repository.JpaRepository

interface AuthEmailRepository : JpaRepository<EmailVerification, String> {
    fun findByEmail(email: String): EmailVerification?
}