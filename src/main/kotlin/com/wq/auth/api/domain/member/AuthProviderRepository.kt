package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthProviderRepository : JpaRepository<AuthProviderEntity, Long> {
    fun findByEmail(email: String): AuthProviderEntity?
}