package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.external.oauth.KakaoOAuthClient
import com.wq.auth.domain.auth.request.SocialLoginRequest
import com.wq.auth.domain.auth.response.SocialLoginResult
import com.wq.auth.security.jwt.JwtProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class KakaoLoginProvider(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val authProviderRepository: AuthProviderRepository,
    private val memberRepository: MemberRepository,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) : AbstractLoginProvider(authProviderRepository, memberRepository) {

    private val log = KotlinLogging.logger {}
    override fun processLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "카카오 소셜 로그인 처리 시작" }

        val oauthUser = kakaoOAuthClient.getUserFromAuthCode(
            request.authCode,
            request.codeVerifier,
            request.redirectUri
        )

        log.info { "OAuth 사용자 정보 조회 완료: ${oauthUser.email}" }

        val (member, isNewMember) = findOrCreateMember(oauthUser, oauthUser.providerType)

        createOrUpdateAuthProvider(member, oauthUser, oauthUser.providerType)

        member.lastLoginAt = LocalDateTime.now()
        memberRepository.save(member)

        val accessToken = jwtProvider.createAccessToken(member.opaqueId, member.role)
        val refreshToken = jwtProvider.createRefreshToken(member.opaqueId)

        val jti = jwtProvider.getJti(refreshToken)
        val opaqueId = jwtProvider.getOpaqueId(refreshToken)
        val refreshTokenEntity = RefreshTokenEntity.of(member, jti, opaqueId)
        refreshTokenRepository.save(refreshTokenEntity)

        log.info { "카카오 소셜 로그인 완료: ${member.opaqueId}, 신규 회원: $isNewMember" }

        return SocialLoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    override fun support(providerType: ProviderType): Boolean {
        return providerType == ProviderType.KAKAO
    }

}
