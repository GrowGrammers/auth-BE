package com.wq.auth.api.domain.member

import com.wq.auth.api.domain.auth.AuthProviderRepository
import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.api.domain.member.error.MemberExceptionCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    ) {

    data class UserInfoResult(
        val nickname: String,
        val email: String,
    )

    @Transactional(readOnly = true)
    fun getUserInfo(opaqueId: String): UserInfoResult {
        val member = memberRepository.findByOpaqueId(opaqueId)
            .orElseThrow { MemberException(MemberExceptionCode.USER_INFO_RETRIEVE_FAILED)}

        val authProviders = authProviderRepository.findByMember(member)
        if (authProviders.isEmpty()) {
            throw MemberException(MemberExceptionCode.USER_INFO_RETRIEVE_FAILED)
        }
        //자체 회원가입에 우선순위를 두고 정보를 주고 있음
        //TODO
        //primaryEmail 가져오는 것으로 변경 + 전화번호 까지
        val email = authProviders
            .sortedBy { provider ->
                when (provider.providerType) {
                    ProviderType.EMAIL -> 0
                    ProviderType.GOOGLE -> 1
                    ProviderType.NAVER -> 2
                    else -> 3
                }
            }
            .first()
            .email

        return UserInfoResult(
            nickname = member.nickname,
            email = email
        )
    }

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
