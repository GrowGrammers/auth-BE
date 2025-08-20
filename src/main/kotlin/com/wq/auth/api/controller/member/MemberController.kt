package com.wq.auth.api.controller.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/members")
class MemberController(private val memberService: MemberService) {

    @GetMapping
    fun getAll(): List<MemberEntity> = memberService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): MemberEntity? = memberService.getById(id)

    @PostMapping
    fun create(@RequestBody member: MemberEntity): MemberEntity = memberService.create(member)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = memberService.delete(id)

    @PutMapping("/{id}/nickname")
    fun updateNickname(
        @PathVariable id: Long,
        @RequestBody payload: Map<String, String>
    ): MemberEntity? {
        val newNickname = payload["nickname"] ?: return null
        return memberService.updateNickname(id, newNickname)
    }

}
