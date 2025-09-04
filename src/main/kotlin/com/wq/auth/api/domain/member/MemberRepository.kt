package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun existsByNickname(nickname: String): Boolean
    fun findByOpaqueId(opaqueId: String): Optional<MemberEntity>
}