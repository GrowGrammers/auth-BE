package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AuthProviderRepository : JpaRepository<AuthProviderEntity, Long> {
    fun findByEmail(email: String): AuthProviderEntity?

    fun findByProviderIdAndProviderType(
        providerId: String,
        providerType: ProviderType
    ): Optional<AuthProviderEntity>

    fun findByMember(member: MemberEntity): List<AuthProviderEntity>

    fun findByMemberAndProviderType(
        member: MemberEntity,
        providerType: ProviderType
    ): Optional<AuthProviderEntity>
}