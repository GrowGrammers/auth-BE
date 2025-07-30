package com.wq.demo.controller

import com.wq.demo.entity.Member
import com.wq.demo.service.MemberService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/members")
class MemberController(private val memberService: MemberService) {

    @GetMapping
    fun getAll(): List<Member> = memberService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Member? = memberService.getById(id)

    @PostMapping
    fun create(@RequestBody member: Member): Member = memberService.create(member)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = memberService.delete(id)
}
