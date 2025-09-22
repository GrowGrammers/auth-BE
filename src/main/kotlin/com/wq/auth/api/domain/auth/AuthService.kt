package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.auth.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.api.domain.auth.error.AuthException
import com.wq.auth.api.domain.auth.error.AuthExceptionCode
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.wq.auth.shared.utils.NicknameGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Service
class AuthService(
    private val authEmailService: AuthEmailService,
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val nicknameGenerator: NicknameGenerator,

    ) {
    private val log = KotlinLogging.logger {}

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String,
    )

    @Transactional
    fun emailLogin(email: String, deviceId: String?): TokenResult {
        val existingUser =
            authProviderRepository.findByEmail(email)?.member ?: return signUp(email, deviceId)

        // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
        val opaqueId = existingUser.opaqueId
        val accessToken =
            jwtProvider.createAccessToken(
                opaqueId = existingUser.opaqueId,
                role = Role.MEMBER,
                extraClaims = mapOf("deviceId" to deviceId)
            )

        val existingRefreshToken = refreshTokenRepository.findActiveByMemberAndDeviceId(existingUser, deviceId)

        //이전 리프레시토큰 soft delete 처리
        if (existingRefreshToken != null) {
            refreshTokenRepository.softDeleteByMemberAndDeviceId(existingUser, deviceId, Instant.now())
        }

        val refreshToken = jwtProvider.createRefreshToken(opaqueId = existingUser.opaqueId)
        val jti = jwtProvider.getJti(refreshToken)

        val refreshTokenEntity = RefreshTokenEntity.of(existingUser, jti, opaqueId, deviceId)
        refreshTokenRepository.save(refreshTokenEntity)
        existingUser.lastLoginAt = LocalDateTime.now()

        return TokenResult(accessToken, refreshToken)

    }

    @Transactional
    fun signUp(email: String, deviceId: String?): TokenResult {

        authEmailService.validateEmailFormat(email)

        var nickname: String
        do {
            nickname = nicknameGenerator.generate()
            //중복 닉네임인 경우
        } while (memberRepository.existsByNickname(nickname))
        val member = MemberEntity.createEmailVerifiedMember(nickname, email)
        val opaqueId = member.opaqueId

        try {
            memberRepository.save(member)
            val provider = AuthProviderEntity.createEmailProvider(member, email)
            authProviderRepository.save(provider)
        } catch (ex: Exception) {
            throw AuthException(AuthExceptionCode.DATABASE_SAVE_FAILED, ex)
        }

        val accessToken = jwtProvider.createAccessToken(
            opaqueId = member.opaqueId,
            role = Role.MEMBER,
            extraClaims = mapOf("deviceId" to deviceId)
        )
        val refreshToken = jwtProvider.createRefreshToken(opaqueId = member.opaqueId)
        val jti = jwtProvider.getJti(refreshToken)

        val refreshTokenEntity = RefreshTokenEntity.of(member, jti, opaqueId, deviceId)
        refreshTokenRepository.save(refreshTokenEntity)

        return TokenResult(accessToken, refreshToken)
    }

    @Transactional
    fun logout(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) {
            log.info { "refreshToken이 없는 상태로 로그아웃 시도" }
            return
        }

        try {
            // 토큰 유효성 검사
            jwtProvider.validateOrThrow(refreshToken)
            
            // 유효한 토큰인 경우 soft delete 처리
            val opaqueId = jwtProvider.getOpaqueId(refreshToken)
            val jti = jwtProvider.getJti(refreshToken)
            refreshTokenRepository.softDeleteByOpaqueIdAndJti(opaqueId, jti, Instant.now())
            
        } catch (e: JwtException) {
            // 만료된 토큰이어도 로그아웃 성공으로 처리
            log.info { "만료된 refreshToken으로 로그아웃: ${e.message}" }
        } catch (ex: Exception) {
            // DB 삭제 실패 시에만 예외 발생
            throw AuthException(AuthExceptionCode.LOGOUT_FAILED, ex)
        }
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String, deviceId: String?): TokenResult {
        //토큰 유효성 검사
        jwtProvider.validateOrThrow(refreshToken)

        val jti = jwtProvider.getJti(refreshToken)
        val opaqueId = jwtProvider.getOpaqueId(refreshToken)

        //토큰 jti+opaqueId로 DB에 있는지 확인
        refreshTokenRepository.findActiveByOpaqueIdAndJti(opaqueId, jti)?: throw JwtException(JwtExceptionCode.MALFORMED)

        //토큰 엔티티 만료 기간 확인
        if (jwtProvider.getRefreshTokenExpiredAt(refreshToken).isBefore(Instant.now())) {
            refreshTokenRepository.softDeleteByOpaqueIdAndJti(opaqueId, jti, Instant.now())
            throw JwtException(JwtExceptionCode.EXPIRED)
        }

        // AccessToken, RefreshToken 재발급
        val newAccessToken = jwtProvider.createAccessToken(
            opaqueId = opaqueId,
            role = Role.MEMBER,
            extraClaims = mapOf("deviceId" to deviceId)
        )
        val newRefreshToken = jwtProvider.createRefreshToken(opaqueId = opaqueId)
        val newJti = jwtProvider.getJti(newRefreshToken)

        // 기존 RefreshToken soft delete 처리
        refreshTokenRepository.softDeleteByOpaqueIdAndJti(opaqueId, jti, Instant.now())

        // 새 refreshToken 저장
        val member = memberRepository.findByOpaqueId(opaqueId).get()
        val newRefreshTokenEntity = RefreshTokenEntity.of(member, newJti, opaqueId, deviceId)
        refreshTokenRepository.save(newRefreshTokenEntity)

        return TokenResult(newAccessToken, newRefreshToken)
    }

}
