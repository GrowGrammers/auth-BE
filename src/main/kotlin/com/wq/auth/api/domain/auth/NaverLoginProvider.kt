package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.api.domain.auth.request.OAuthAuthCodeRequest
import com.wq.auth.api.domain.auth.request.SocialLoginRequest
import com.wq.auth.api.domain.auth.response.SocialLoginResult
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.external.oauth.NaverOAuthClient
import com.wq.auth.security.jwt.JwtProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NaverLoginProvider(
    private val naverOAuthClient: NaverOAuthClient,
    private val authProviderRepository: AuthProviderRepository,
    private val memberRepository: MemberRepository,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) : AbstractLoginProvider(authProviderRepository, memberRepository) {
    private val log = KotlinLogging.logger {}

    override fun processLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "Naver 소셜 로그인 처리 시작" }

        // 1. 네이버 OAuth 클라이언트를 통해 사용자 정보 조회
        val oauthUser = naverOAuthClient.getUserFromAuthCode(
            OAuthAuthCodeRequest(
                authCode = request.authCode,
                state = request.state!!,      // 네이버는 state 사용
                codeVerifier = request.codeVerifier
            )
        )

        log.info { "OAuth 사용자 정보 조회 완료: ${oauthUser.email}" }

        // 2. 기존 회원 확인 또는 신규 회원 생성
        val (member, isNewMember) = findOrCreateMember(oauthUser, oauthUser.providerType)

        // 3. AuthProvider 엔티티 생성/업데이트
        createOrUpdateAuthProvider(member, oauthUser, oauthUser.providerType)

        // 4. 로그인 시간 업데이트
        member.lastLoginAt = LocalDateTime.now()
        memberRepository.save(member)

        // 5. JWT 토큰 발급
        val accessToken = jwtProvider.createAccessToken(member.opaqueId, member.role)
        val refreshToken = jwtProvider.createRefreshToken(member.opaqueId)

        // 6. RefreshToken 저장
        val jti = jwtProvider.getJti(refreshToken)
        val opaqueId = jwtProvider.getOpaqueId(refreshToken)
        val refreshTokenEntity = RefreshTokenEntity.of(member, jti, opaqueId)
        refreshTokenRepository.save(refreshTokenEntity)

        log.info { "소셜 로그인 완료: ${member.opaqueId}, 신규 회원: $isNewMember" }

        return SocialLoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    override fun support(providerType: ProviderType): Boolean {
        return providerType == ProviderType.NAVER
    }
}