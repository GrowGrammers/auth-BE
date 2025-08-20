package com.wq.demo.email

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "email_verification")
data class EmailVerificationEntity(
    @Id
    val email: String,
    val code: String
)