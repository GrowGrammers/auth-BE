package com.wq.auth.api.controller.member

import com.wq.auth.api.controller.member.request.EmailLoginRequestDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.email.error.EmailException
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.web.common.response.BaseResponse
import com.wq.auth.web.common.response.Responses
import org.springframework.web.bind.annotation.*

@RestController
class MemberController(private val memberService: MemberService,
    private val emailService: AuthEmailService
) {

    //apiDocs 추가
    @PostMapping("api/v1/auth/members/email-login")
    fun emailLogin(@RequestBody req: EmailLoginRequestDto): BaseResponse {
        return try{
            emailService.verifyCode(req.email, req.code)
            val resp = memberService.emailLogin(req.email)
            Responses.success(message = "로그인에 성공했습니다.", data = resp)
            //멤버 에러 처리 추가
        } catch (e: EmailException) {
            //이메일 인증 실패 에러나 멤버 에러를 같이 쓸 수 있어야 함
            Responses.fail(e.emailCode)
        }
    }

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
