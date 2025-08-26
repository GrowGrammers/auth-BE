package com.wq.auth.api.domain.member

import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.shared.utils.NicknameGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val authEmailService: AuthEmailService,
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    private val jwtProvider: JwtProvider,
    private val nicknameGenerator: NicknameGenerator,
    private val jwtProperties: JwtProperties

) {
    @Transactional
    fun emailLogin(email: String): LoginResponseDto {
        val existingUser = authProviderRepository.findByEmail(email)

        return if (existingUser != null) {
            // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
            val accessToken = jwtProvider.createAccessToken(subject = existingUser.member.id.toString())
            val refreshToken = jwtProvider.createRefreshToken(subject = existingUser.member.id.toString())
            //리프레시 토큰 db에 넣어두고 비교 할 때 사용해야함
            //까보고 그냥 멤버로 줘도 되는거 아닌가
            //그래도 있어야지..
            val expiredAt = jwtProperties.accessExp.toMillis() + System.currentTimeMillis()
            LoginResponseDto.fromTokens(accessToken, refreshToken, expiredAt)
        } else {
            // 없는 이메일 → 회원가입 로직
            signUp(email)
        }
    }

    @Transactional
    fun signUp(email: String): LoginResponseDto {

        authEmailService.validateEmailFormat(email)

        var nickname: String
        do {
            nickname = nicknameGenerator.generate()
            //중복 닉네임인 경우
        } while (memberRepository.existsByNickname(nickname))

        val member = MemberEntity.createEmailVerifiedMember(nickname)
        memberRepository.save(member)

        val provider = AuthProviderEntity.createEmailProvider(member, email)
        authProviderRepository.save(provider)

        val accessToken = jwtProvider.createAccessToken(subject = member.id.toString())
        val refreshToken = jwtProvider.createRefreshToken(subject = member.id.toString())
        val expiredAt = jwtProperties.accessExp.toMillis() + System.currentTimeMillis()

        return LoginResponseDto.fromTokens(accessToken, refreshToken, expiredAt)
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
