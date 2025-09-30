package com.wq.auth.domain.auth

import com.wq.auth.api.domain.auth.AuthProviderRepository
import com.wq.auth.api.domain.auth.RefreshTokenRepository
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.auth.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.domain.auth.request.SocialLoginRequest
import com.wq.auth.domain.auth.response.SocialLoginResult
import com.wq.auth.api.external.oauth.GoogleOAuthClient
import com.wq.auth.api.external.oauth.KakaoOAuthClient
import com.wq.auth.api.external.oauth.NaverOAuthClient
import com.wq.auth.domain.auth.request.OAuthAuthCodeRequest
import com.wq.auth.domain.oauth.OAuthUser
import com.wq.auth.domain.oauth.error.SocialLoginException
import com.wq.auth.domain.oauth.error.SocialLoginExceptionCode
import com.wq.auth.security.jwt.JwtProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 소셜 로그인 서비스
 *
 * 소셜 로그인의 전체 플로우를 관리합니다:
 * 1. 소셜 제공자로부터 사용자 정보 조회
 * 2. 기존 회원 확인 또는 신규 회원 생성
 * 3. AuthProvider 엔티티 생성/업데이트
 * 4. JWT 토큰 발급
 */
@Service
@Transactional(readOnly = true)
class SocialLoginService(
    private val googleOAuthClient: GoogleOAuthClient,
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val naverOAuthClient: NaverOAuthClient,
    private val memberRepository: MemberRepository,
    private val authProviderRepository: AuthProviderRepository,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    private val log = KotlinLogging.logger {}

    /**
     * 소셜 로그인을 처리합니다.
     *
     * @param request 소셜 로그인 요청 DTO
     * @return 소셜 로그인 응답 DTO (JWT 토큰 포함)
     */
    @Transactional
    fun processSocialLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "소셜 로그인 처리 시작: ${request.providerType}" }

        return when (request.providerType) {
            ProviderType.GOOGLE -> processGoogleLogin(request)
            ProviderType.KAKAO -> processKakaoLogin(request)
            ProviderType.NAVER -> processNaverLogin(request)
            ProviderType.EMAIL -> throw SocialLoginException(SocialLoginExceptionCode.UNSUPPORTED_PROVIDER)
            ProviderType.PHONE -> throw SocialLoginException(SocialLoginExceptionCode.UNSUPPORTED_PROVIDER)
        }
    }

    /**
     * 카카오 소셜 로그인을 처리합니다.
     */
    private fun processKakaoLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "카카오 소셜 로그인 처리 시작" }

        // 1. 카카오 OAuth 클라이언트를 통해 사용자 정보 조회
        val oauthUser = kakaoOAuthClient.getUserFromAuthCode(
            OAuthAuthCodeRequest(
                request.authCode,
                request.codeVerifier,
                request.redirectUri
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

        log.info { "카카오 소셜 로그인 완료: ${member.opaqueId}, 신규 회원: $isNewMember" }

        return SocialLoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    /**
     * Google 소셜 로그인을 처리합니다.
     */
    private fun processGoogleLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "Google 소셜 로그인 처리 시작" }

        // 1. 구글 OAuth 클라이언트를 통해 사용자 정보 조회
        val oauthUser = googleOAuthClient.getUserFromAuthCode(
            OAuthAuthCodeRequest(
                authCode = request.authCode,
                codeVerifier = request.codeVerifier,
                redirectUri = request.redirectUri
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

    /**
     * Naver 소셜 로그인을 처리합니다.
     */
    private fun processNaverLogin(request: SocialLoginRequest): SocialLoginResult {
        log.info { "Naver 소셜 로그인 처리 시작" }

        // 1. 네이버 OAuth 클라이언트를 통해 사용자 정보 조회
        val oauthUser = naverOAuthClient.getUserFromAuthCode(
            OAuthAuthCodeRequest(
                authCode = request.authCode,
                state = request.state!!,      // 네이버는 state 사용
                codeVerifier = request.codeVerifier,
                redirectUri = request.redirectUri
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

    /**
     * 기존 회원을 찾거나 신규 회원을 생성합니다.
     *
     * @param oauthUser OAuth 사용자 정보
     * @param providerType 소셜 제공자 타입
     * @return Pair<회원 엔티티, 신규 회원 여부>
     */
    private fun findOrCreateMember(
        oauthUser: OAuthUser,
        providerType: ProviderType
    ): Pair<MemberEntity, Boolean> {

        // AuthProvider 테이블에서 기존 회원 확인
        val existingAuthProvider = authProviderRepository.findByProviderIdAndProviderType(
            oauthUser.providerId,
            providerType
        )

        if (existingAuthProvider.isPresent) {
            log.info { "기존 회원 발견: ${existingAuthProvider.get().member.opaqueId}" }
            return Pair(existingAuthProvider.get().member, false)
        }

        // 신규 회원 생성
        log.info { "신규 회원 생성: ${oauthUser.email}" }
        val newMember = MemberEntity.Companion.createSocialMember(
            nickname = oauthUser.getNickname(),
            isEmailVerified = oauthUser.verifiedEmail,
            primaryEmail = oauthUser.email
        )

        val savedMember = memberRepository.save(newMember)
        log.info { "신규 회원 생성 완료: ${savedMember.opaqueId}" }

        return Pair(savedMember, true)
    }

    /**
     * AuthProvider 엔티티를 생성하거나 업데이트합니다.
     *
     * @param member 회원 엔티티
     * @param oauthUser OAuth 사용자 정보
     * @param providerType 소셜 제공자 타입
     */
    private fun createOrUpdateAuthProvider(
        member: MemberEntity,
        oauthUser: OAuthUser,
        providerType: ProviderType
    ) {
        val existingAuthProvider = authProviderRepository.findByMemberAndProviderType(member, providerType)

        if (existingAuthProvider.isPresent) {
            // 기존 AuthProvider 업데이트
            val authProvider = existingAuthProvider.get()
            // providerId와 email을 업데이트하는 메서드 호출 (엔티티에 setter 메서드가 있어야 함)
            authProvider.updateProviderInfo(oauthUser.providerId, oauthUser.email)
            authProviderRepository.save(authProvider)
            log.info { "AuthProvider 업데이트 완료: ${member.opaqueId}" }
        } else {
            // 새로운 AuthProvider 생성
            val authProvider = AuthProviderEntity(
                member = member,
                providerType = providerType,
                providerId = oauthUser.providerId,
                email = oauthUser.email
            )
            authProviderRepository.save(authProvider)
            log.info { "AuthProvider 생성 완료: ${member.opaqueId}" }
        }
    }
}