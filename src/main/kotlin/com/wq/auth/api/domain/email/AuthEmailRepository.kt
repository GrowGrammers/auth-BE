package com.wq.auth.api.domain.email

import com.wq.auth.api.domain.email.entity.EmailVerificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthEmailRepository : JpaRepository<EmailVerificationEntity, String> {
    fun findFirstByEmailOrderByCreatedAtDesc(email: String): EmailVerificationEntity?
}