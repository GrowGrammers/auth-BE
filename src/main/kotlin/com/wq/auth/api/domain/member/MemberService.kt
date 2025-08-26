package com.wq.auth.api.domain.member

import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.shared.utils.NicknameGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    private val jwtProvider: JwtProvider,
    private val nicknameGenerator: NicknameGenerator,
    private val jwtProperties: JwtProperties

) {
    @Transactional
    fun emailLogin(email: String): LoginResponseDto {
        val email = email
        val existingUser = authProviderRepository.findByEmail(email)

        return if (existingUser != null) {
            // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
            val accessToken = jwtProvider.createAccessToken(
                subject = existingUser.member.id.toString(),
                //굳이 닉네임을 넣어줘야하나
                extraClaims = mapOf("nickname" to existingUser.member.nickname)
            )
            val refreshToken = jwtProvider.createRefreshToken(subject = existingUser.member.id.toString())
            //리프레시 토큰 db에 넣어두고 비교 할 때 사용해야함
            //까보고 그냥 멤버로 줘도 되는거 아닌가
            //그래도 있어야지..
            val expiredAt = jwtProperties.accessExp.toMillis() + System.currentTimeMillis()

            //만드는 함수를 추가해두고 쓸지
            LoginResponseDto(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiredAt = expiredAt
            )
        } else {
            // 없는 이메일 → 회원가입 로직
            signUp(email)
        }
    }

    @Transactional
    fun signUp(email: String): LoginResponseDto {
        var nickname: String
        do {
            nickname = nicknameGenerator.generate()
        } while (memberRepository.existsByNickname(nickname))

        //이메일 인증 db에 해당 이메일로 된 컬럼이 없는 경우
        //에러 반환

        // 이메일 인증 성공시
        val member = MemberEntity(
            nickname = nickname,
            isEmailVerified = true
        )

        memberRepository.save(member)

        val provider = AuthProviderEntity(
            member = member,
            providerType = ProviderType.valueOf(ProviderType.EMAIL.toString()),
            email = email
        )
        authProviderRepository.save(provider)

        val accessToken = jwtProvider.createAccessToken(
            subject = member.id.toString(),
            extraClaims = mapOf("nickname" to nickname)
        )
        val refreshToken = jwtProvider.createRefreshToken(subject = member.id.toString())
        val expiredAt = jwtProperties.accessExp.toMillis() + System.currentTimeMillis()

        return LoginResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiredAt = expiredAt
        )
        
        //에러 처리 추가
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
