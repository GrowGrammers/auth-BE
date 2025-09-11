package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberRepository : JpaRepository<MemberEntity, Long> {

    fun existsByNickname(nickname: String): Boolean
    fun findByOpaqueId(opaqueId: String): Optional<MemberEntity>

    fun findByProviderId(providerId: String): Optional<MemberEntity>

    @Query("""
        SELECT m FROM MemberEntity m 
        JOIN AuthProviderEntity ap ON m.id = ap.member.id 
        WHERE ap.providerId = :providerId 
        AND ap.providerType = :providerType 
        AND m.isDeleted = false
    """)
    fun findByProviderIdAndProviderType(
        @Param("providerId") providerId: String,
        @Param("providerType") providerType: ProviderType
    ): Optional<MemberEntity>

    fun findByOpaqueIdAndIsDeletedFalse(opaqueId: String): Optional<MemberEntity>
}