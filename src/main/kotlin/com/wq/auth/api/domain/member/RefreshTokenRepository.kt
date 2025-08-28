package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.refreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface RefreshTokenRepository : JpaRepository<refreshTokenEntity, Long> {
    fun findByJti(jti: String): refreshTokenEntity?
    fun findByMember(member: MemberEntity): refreshTokenEntity?

    @Transactional
    @Modifying
    @Query("delete from refreshTokenEntity r where r.member.id = :memberId and r.jti = :jti")
    fun deleteByMemberIdAndJti(memberId: Long, jti: String)
}
