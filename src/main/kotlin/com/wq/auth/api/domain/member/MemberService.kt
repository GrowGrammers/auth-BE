package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    ) {

    fun getAll(): List<MemberEntity> = memberRepository.findAll()

    fun getById(id: Long): MemberEntity? = memberRepository.findById(id).orElse(null)

    fun create(member: MemberEntity): MemberEntity = memberRepository.save(member)

    fun delete(id: Long) = memberRepository.deleteById(id)

    fun updateNickname(id: Long, newNickname: String): MemberEntity? {
        val member = memberRepository.findById(id).orElse(null)
        member?.let {
            it.nickname = newNickname
            return memberRepository.save(it)
        }
        return null
    }

}
