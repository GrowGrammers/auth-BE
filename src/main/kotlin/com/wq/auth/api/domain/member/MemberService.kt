package com.wq.auth.api.domain.member

import com.wq.auth.api.controller.member.response.LoginResponseDto
import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.api.domain.member.error.MemberExceptionCode
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.jwt.error.JwtException
import com.wq.auth.jwt.error.JwtExceptionCode
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

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiredAt: Long
    )

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

            val refreshTokenEntity = RefreshTokenEntity.of(existingUser, jti, refreshTokenExpiredAt)
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

        val refreshTokenEntity = RefreshTokenEntity.of(member, jti, refreshTokenExpiredAt)
        refreshTokenRepository.save(refreshTokenEntity)

        return LoginResponseDto.fromTokens(accessToken, refreshToken, accessTokenExpiredAt)
    }

    @Transactional
    fun logout(refreshToken: String) {
        try {
            val tokenMemberId = jwtProvider.getSubject(refreshToken).toLong()
            val jti = jwtProvider.getJti(refreshToken)
            refreshTokenRepository.deleteByMemberIdAndJti(tokenMemberId, jti)
        } catch (ex: Exception) {
            throw MemberException(MemberExceptionCode.LOGOUT_FAILED, ex)
        }
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): TokenResult {
        //토큰 유효성 검사
        jwtProvider.validateOrThrow(refreshToken)

        val jti = jwtProvider.getJti(refreshToken)
        val memberId = jwtProvider.getSubject(refreshToken).toLong()

        //토큰 jti+memberId로 DB에 있는지 확인
        val refreshTokenEntity = refreshTokenRepository.findByMemberIdAndJti(memberId, jti)
            ?: throw MemberException(MemberExceptionCode.REFRESHTOKEN_DATABASE_FIND_FAILED)

        //토큰 엔티티 만료 기간 확인
        if(refreshTokenEntity.expiredAt.isBefore(Instant.now())){
            refreshTokenRepository.delete(refreshTokenEntity)
            throw JwtException(JwtExceptionCode.EXPIRED)
        }

        // 5. AccessToken, RefreshToken 재발급
        val newAccessToken = jwtProvider.createAccessToken(subject = memberId.toString())
        val (newRefreshToken, newJti) = jwtProvider.createRefreshToken(subject = memberId.toString())

        // 기존 RefreshToken 삭제
        refreshTokenRepository.delete(refreshTokenEntity)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)
        val member = memberRepository.findById(memberId).get()

        val newRefreshTokenEntity = RefreshTokenEntity.of(member, newJti, refreshTokenExpiredAt)
        refreshTokenRepository.save(newRefreshTokenEntity)

        return TokenResult(newAccessToken, newRefreshToken, accessTokenExpiredAt)
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
