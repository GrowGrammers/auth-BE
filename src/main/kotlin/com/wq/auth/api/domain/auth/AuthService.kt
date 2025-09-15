package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.auth.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.api.domain.auth.error.AuthException
import com.wq.auth.api.domain.auth.error.AuthExceptionCode
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.wq.auth.shared.utils.NicknameGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
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
        val accessTokenExpiredAt: Long,
        val refreshTokenExpiredAt: Long
    )

    @Transactional
    fun emailLogin(email: String, deviceId: String?, clientType: String): TokenResult {
        val existingUser =
            authProviderRepository.findByEmail(email)?.member ?: return signUp(email, deviceId, clientType)

        // 이미 가입된 사용자 → 로그인 처리 및 JWT 발급
        val opaqueId = existingUser.opaqueId
        val accessToken =
            jwtProvider.createAccessToken(
                opaqueId = existingUser.opaqueId,
                role = Role.MEMBER,
                extraClaims = mapOf("deviceId" to deviceId)
            )

        val existingRefreshToken = refreshTokenRepository.findByMemberAndDeviceId(existingUser, deviceId)

        //이전 리프레시토큰 삭제
        if (existingRefreshToken != null) {
            refreshTokenRepository.delete(existingRefreshToken)
        }

        val refreshToken = jwtProvider.createRefreshToken(opaqueId = existingUser.opaqueId)
        val jti = jwtProvider.getJti(refreshToken)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

        val refreshTokenEntity: RefreshTokenEntity
        if (clientType == "app") {
            refreshTokenEntity =
                RefreshTokenEntity.ofApp(existingUser, jti, refreshTokenExpiredAt, opaqueId, deviceId!!)
        } else {
            refreshTokenEntity = RefreshTokenEntity.ofWeb(existingUser, jti, refreshTokenExpiredAt, opaqueId)
        }
        refreshTokenRepository.save(refreshTokenEntity)

        return TokenResult(accessToken, refreshToken, accessTokenExpiredAt, refreshTokenExpiredAt.toEpochMilli())

    }

    @Transactional
    fun signUp(email: String, deviceId: String?, clientType: String): TokenResult {

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
            throw AuthException(AuthExceptionCode.DATABASE_SAVE_FAILED, ex)
        }

        val accessToken = jwtProvider.createAccessToken(
            opaqueId = member.opaqueId,
            role = Role.MEMBER,
            extraClaims = mapOf("deviceId" to deviceId)
        )
        val refreshToken = jwtProvider.createRefreshToken(opaqueId = member.opaqueId)
        val jti = jwtProvider.getJti(refreshToken)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)

        val refreshTokenEntity: RefreshTokenEntity
        if (clientType == "app") {
            refreshTokenEntity = RefreshTokenEntity.ofApp(member, jti, refreshTokenExpiredAt, opaqueId, deviceId!!)
        } else {
            refreshTokenEntity = RefreshTokenEntity.ofWeb(member, jti, refreshTokenExpiredAt, opaqueId)
        }
        refreshTokenRepository.save(refreshTokenEntity)

        return TokenResult(accessToken, refreshToken, accessTokenExpiredAt, refreshTokenExpiredAt.toEpochMilli())
    }

    @Transactional
    fun logout(refreshToken: String) {
        val opaqueId = jwtProvider.getOpaqueId(refreshToken)
        val jti = jwtProvider.getJti(refreshToken)
        try {
            refreshTokenRepository.deleteByOpaqueIdAndJti(opaqueId, jti)
        } catch (ex: Exception) {
            throw AuthException(AuthExceptionCode.LOGOUT_FAILED, ex)
        }
    }

    //TODO
    //모바일 재발급의 경우
    @Transactional
    fun refreshAccessToken(refreshToken: String, deviceId: String?, clientType: String): TokenResult {
        //토큰 유효성 검사
        jwtProvider.validateOrThrow(refreshToken)

        val jti = jwtProvider.getJti(refreshToken)
        val opaqueId = jwtProvider.getOpaqueId(refreshToken)

        //토큰 jti+opaqueId로 DB에 있는지 확인
        val refreshTokenEntity = refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)
            ?: throw JwtException(JwtExceptionCode.MALFORMED)

        //토큰 엔티티 만료 기간 확인
        if (refreshTokenEntity.expiredAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity)
            throw JwtException(JwtExceptionCode.EXPIRED)
        }

        // 5. AccessToken, RefreshToken 재발급
        val newAccessToken = jwtProvider.createAccessToken(
            opaqueId = opaqueId,
            role = Role.MEMBER,
            extraClaims = mapOf("deviceId" to deviceId)
        )
        val newRefreshToken = jwtProvider.createRefreshToken(opaqueId = opaqueId)
        val newJti = jwtProvider.getJti(newRefreshToken)

        // 기존 RefreshToken 삭제
        refreshTokenRepository.delete(refreshTokenEntity)

        val now = System.currentTimeMillis()
        val accessTokenExpiredAt = now + jwtProperties.accessExp.toMillis()
        val refreshTokenExpiredAt = Instant.now().plus(jwtProperties.refreshExp)
        val member = memberRepository.findByOpaqueId(opaqueId).get()

        val newRefreshTokenEntity: RefreshTokenEntity
        if (clientType == "app") {
            newRefreshTokenEntity =
                RefreshTokenEntity.ofApp(member, newJti, refreshTokenExpiredAt, opaqueId, deviceId!!)
        } else {
            newRefreshTokenEntity = RefreshTokenEntity.ofWeb(member, newJti, refreshTokenExpiredAt, opaqueId)
        }
        refreshTokenRepository.save(newRefreshTokenEntity)

        return TokenResult(newAccessToken, newRefreshToken, accessTokenExpiredAt, refreshTokenExpiredAt.toEpochMilli())
    }

}
