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

    @Transactional
    fun emailLogin(email: String): LoginResponseDto {
        val existingUser = authProviderRepository.findByEmail(email)?.member ?: return signUp(email)

        //신규사용자
        val opaqueId = existingUser.opaqueId
        // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
        val accessToken = jwtProvider.createAccessToken(subject = existingUser.opaqueId)
        //TODO
        //다양한 디바이스 지원시 바뀌어야할지도
        val existingRefreshToken = refreshTokenRepository.findByMember(existingUser)

        //이전 리프레시토큰 삭제
        if (existingRefreshToken != null) {
            refreshTokenRepository.delete(existingRefreshToken)
        }

        val (refreshToken, jti) = jwtProvider.createRefreshToken(subject = existingUser.opaqueId)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

        val refreshTokenEntity = RefreshTokenEntity.of(existingUser, jti, refreshTokenExpiredAt, opaqueId)
        refreshTokenRepository.save(refreshTokenEntity)

        return LoginResponseDto.fromTokens(accessToken, refreshToken, accessTokenExpiredAt)

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
        val opaqueId = member.opaqueId

        try {
            memberRepository.save(member)
            val provider = AuthProviderEntity.createEmailProvider(member, email)
            authProviderRepository.save(provider)
        } catch (ex: Exception) {
            throw MemberException(MemberExceptionCode.DATABASE_SAVE_FAILED, ex)
        }

        val accessToken = jwtProvider.createAccessToken(subject = member.opaqueId)
        val (refreshToken, jti) = jwtProvider.createRefreshToken(subject = member.opaqueId)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

        val refreshTokenEntity = RefreshTokenEntity.of(member, jti, refreshTokenExpiredAt, opaqueId)
        refreshTokenRepository.save(refreshTokenEntity)

        return LoginResponseDto.fromTokens(accessToken, refreshToken, accessTokenExpiredAt)
    }

    @Transactional
    fun logout(refreshToken: String) {
        try {
            val opaqueId = jwtProvider.getSubject(refreshToken)
            val jti = jwtProvider.getJti(refreshToken)
            refreshTokenRepository.deleteByOpaqueIdAndJti(opaqueId, jti)
        } catch (ex: Exception) {
            throw MemberException(MemberExceptionCode.LOGOUT_FAILED, ex)
        }
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): TokenResult {
        //토큰 유효성 검사
        jwtProvider.validateOrThrow(refreshToken)

        val jti = jwtProvider.getJti(refreshToken)
        val opaqueId = jwtProvider.getSubject(refreshToken)

        //토큰 jti+opaqueId로 DB에 있는지 확인
        val refreshTokenEntity = refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)
            ?: throw MemberException(MemberExceptionCode.REFRESHTOKEN_DATABASE_FIND_FAILED)

        //토큰 엔티티 만료 기간 확인
        if (refreshTokenEntity.expiredAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity)
            throw JwtException(JwtExceptionCode.EXPIRED)
        }

        // 5. AccessToken, RefreshToken 재발급
        val newAccessToken = jwtProvider.createAccessToken(subject = opaqueId)
        val (newRefreshToken, newJti) = jwtProvider.createRefreshToken(subject = opaqueId)

        // 기존 RefreshToken 삭제
        refreshTokenRepository.delete(refreshTokenEntity)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)
        val member = memberRepository.findByOpaqueId(opaqueId).get()

        val newRefreshTokenEntity = RefreshTokenEntity.of(member, newJti, refreshTokenExpiredAt, opaqueId)
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
