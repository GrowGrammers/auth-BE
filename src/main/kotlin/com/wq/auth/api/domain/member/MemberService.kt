package com.wq.auth.api.domain.member

import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.refreshTokenEntity
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.api.domain.member.error.MemberExceptionCode
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.shared.utils.NicknameGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MemberService(
    private val authEmailService: AuthEmailService,
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val nicknameGenerator: NicknameGenerator,
    private val jwtProperties: JwtProperties,

) {
    @Transactional
    fun emailLogin(email: String): LoginResponseDto {
        val existingUser = authProviderRepository.findByEmail(email)?.member
        // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
        return if (existingUser != null) {
            val accessToken = jwtProvider.createAccessToken(subject = existingUser.id.toString())
            val existingRefreshToken = refreshTokenRepository.findByMember(existingUser)

            //이전 리프레시토큰 삭제
            if (existingRefreshToken != null) {
                refreshTokenRepository.delete(existingRefreshToken)
            }

            val (refreshToken, jti) = jwtProvider.createRefreshToken(subject = existingUser.id.toString())

            val now = System.currentTimeMillis()
            val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
            val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

            val refreshTokenEntity = refreshTokenEntity.of(existingUser, jti, refreshTokenExpiredAt)
            refreshTokenRepository.save(refreshTokenEntity)

            LoginResponseDto.fromTokens(accessToken, refreshToken, accessTokenExpiredAt)
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

        try {
            memberRepository.save(member)
            val provider = AuthProviderEntity.createEmailProvider(member, email)
            authProviderRepository.save(provider)
        } catch (ex: Exception) {
            throw MemberException(MemberExceptionCode.DATABASE_SAVE_FAILED, ex)
        }

        val accessToken = jwtProvider.createAccessToken(subject = member.id.toString())
        val (refreshToken, jti) = jwtProvider.createRefreshToken(subject = member.id.toString())

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

        val refreshTokenEntity = refreshTokenEntity.of(member, jti, refreshTokenExpiredAt)
        refreshTokenRepository.save(refreshTokenEntity)

        return LoginResponseDto.fromTokens(accessToken, refreshToken, accessTokenExpiredAt)
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
