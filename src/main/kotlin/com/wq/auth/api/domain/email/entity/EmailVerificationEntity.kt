package com.wq.auth.api.domain.email.entity

import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.Instant

@Entity
@Table(name = "email_verification")
data class EmailVerificationEntity(
    @Id
    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(nullable = false, length = 10)
    val code: String,

): BaseEntity()