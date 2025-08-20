package com.wq.auth.api.controller.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(private val memberService: MemberService) {

    @GetMapping("/members")
    fun getAll(): List<MemberEntity> = memberService.getAll()

    @GetMapping("/members/{id}")
    fun getById(@PathVariable id: Long): MemberEntity? = memberService.getById(id)

    @PostMapping("/members")
    fun create(@RequestBody member: MemberEntity): MemberEntity = memberService.create(member)

    @DeleteMapping("/members/{id}")
    fun delete(@PathVariable id: Long) = memberService.delete(id)

    @PutMapping("/members/{id}/nickname")
    fun updateNickname(
        @PathVariable id: Long,
        @RequestBody payload: Map<String, String>
    ): MemberEntity? {
        val newNickname = payload["nickname"] ?: return null
        return memberService.updateNickname(id, newNickname)
    }

}
