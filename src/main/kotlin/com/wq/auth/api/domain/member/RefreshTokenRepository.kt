package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.refreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<refreshTokenEntity, Long> {
    fun findByJti(token: String): refreshTokenEntity?
    fun findByMember(member: MemberEntity): refreshTokenEntity?
    fun deleteByMember(member: MemberEntity): Unit
}
