package com.wq.demo.service

import com.wq.demo.entity.Member
import com.wq.demo.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(private val memberRepository: MemberRepository) {

    fun getAll(): List<Member> = memberRepository.findAll()

    fun getById(id: Long): Member? = memberRepository.findById(id).orElse(null)

    fun create(member: Member): Member = memberRepository.save(member)

    fun delete(id: Long) = memberRepository.deleteById(id)
}
