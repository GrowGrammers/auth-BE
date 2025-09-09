package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByJti(jti: String): RefreshTokenEntity?
    fun findByMember(member: MemberEntity): RefreshTokenEntity?

    @Transactional
    @Modifying
    @Query("delete from RefreshTokenEntity r where r.member.opaqueId = :opaqueId and r.jti = :jti")
    fun deleteByOpaqueIdAndJti(opaqueId: String, jti: String)

    fun findByMemberAndDeviceId(member: MemberEntity, deviceId: String?): RefreshTokenEntity?

    @Query("select r from RefreshTokenEntity r where r.member.opaqueId = :opaqueId and r.jti = :jti")
    fun findByOpaqueIdAndJti(opaqueId: String, jti: String): RefreshTokenEntity?

    @Query("select r from RefreshTokenEntity r where r.member.opaqueId = :opaqueId and r.jti = :jti and r.deviceId = :deviceId")
    fun findByOpaqueIdAndJtiAndDeviceId(opaqueId: String, jti: String, deviceId: String?): RefreshTokenEntity?
}
