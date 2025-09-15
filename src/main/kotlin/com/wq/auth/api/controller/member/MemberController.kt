package com.wq.auth.api.controller.member

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.web.common.response.Responses
import com.wq.auth.web.common.response.SuccessResponse
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(
    private val memberService: MemberService,
) {

    @GetMapping("/api/v1/members")
    fun getAll(): SuccessResponse<List<MemberEntity>> =
        Responses.success("회원 목록 조회 성공", memberService.getAll())

    @GetMapping("/api/v1/members/{id}")
    fun getById(@PathVariable id: Long): SuccessResponse<MemberEntity?> =
        Responses.success("회원 조회 성공", memberService.getById(id))

    @PostMapping("/api/v1/members")
    fun create(@RequestBody member: MemberEntity): SuccessResponse<MemberEntity> =
        Responses.success("회원 생성 성공", memberService.create(member))

    @DeleteMapping("/api/v1/members/{id}")
    fun delete(@PathVariable id: Long): SuccessResponse<Void> {
        memberService.delete(id)
        return Responses.success("회원 삭제 성공")
    }

    @PutMapping("/api/v1/members/{id}/nickname")
    fun updateNickname(
        @PathVariable id: Long,
        @RequestBody payload: Map<String, String>
    ): SuccessResponse<MemberEntity?> {
        val newNickname = payload["nickname"] ?: throw IllegalArgumentException("닉네임은 필수입니다")
        return Responses.success("닉네임 변경 성공", memberService.updateNickname(id, newNickname))
    }

}
