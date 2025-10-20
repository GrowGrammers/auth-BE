package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.request.OAuthAuthCodeRequest
import com.wq.auth.api.domain.auth.request.SocialLinkRequest
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.external.oauth.NaverOAuthClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 네이버 계정 연동 Provider
 */
@Component
class NaverLinkProvider(
    private val naverOAuthClient: NaverOAuthClient,
    authProviderRepository: AuthProviderRepository,
    memberRepository: MemberRepository,
) : AbstractLinkProvider(authProviderRepository, memberRepository) {

    private val log = KotlinLogging.logger {}

    override fun processLink(currentMember: MemberEntity, linkRequest: SocialLinkRequest) {
        log.info { "======================================" }
        log.info { "네이버 계정 연동 처리 시작" }
        log.info { "======================================" }
        log.info { "현재 회원 opaqueId: ${currentMember.opaqueId}" }
        log.info { "현재 회원 이메일: ${currentMember.primaryEmail}" }

        // 요청 파라미터 로그
        log.info { "--- 요청 파라미터 ---" }
        log.info { "authCode 길이: ${linkRequest.authCode.length}" }
        log.info { "authCode 앞 10자: ${linkRequest.authCode.take(10)}..." }
        log.info { "codeVerifier: ${linkRequest.codeVerifier ?: "null"}" }
        log.info { "codeVerifier 길이: ${linkRequest.codeVerifier?.length ?: 0}" }
        log.info { "state: ${linkRequest.state ?: "null"}" }
        log.info { "state 길이: ${linkRequest.state?.length ?: 0}" }
        log.info { "providerType: ${linkRequest.providerType}" }

        // 네이버 OAuth2로부터 사용자 정보 조회
        log.info { "--- OAuth 요청 생성 ---" }
        val authCodeRequest = OAuthAuthCodeRequest(
            authCode = linkRequest.authCode,
            codeVerifier = linkRequest.codeVerifier,
            state = linkRequest.state
        )
        log.info { "OAuthAuthCodeRequest 생성 완료" }
        log.info { "authCode: ${authCodeRequest.authCode.take(10)}..." }
        log.info { "codeVerifier: ${authCodeRequest.codeVerifier}" }
        log.info { "state: ${authCodeRequest.state}" }

        log.info { "--- 네이버 OAuth Client 호출 시작 ---" }
        val oauthUser = naverOAuthClient.getUserFromAuthCode(authCodeRequest)

        log.info { "--- 네이버 사용자 정보 조회 성공 ---" }
        log.info { "providerId: ${oauthUser.providerId}" }
        log.info { "email: ${oauthUser.email}" }
        log.info { "verifiedEmail: ${oauthUser.verifiedEmail}" }
        log.info { "name: ${oauthUser.name}" }
        log.info { "givenName: ${oauthUser.givenName}" }
        log.info { "providerType: ${oauthUser.providerType}" }

        // 계정 연동 처리
        log.info { "--- 계정 연동 처리 시작 ---" }
        linkAccount(currentMember, oauthUser, ProviderType.NAVER)

        log.info { "======================================" }
        log.info { "네이버 계정 연동 완료: ${currentMember.opaqueId}" }
        log.info { "======================================" }
    }

    override fun support(providerType: ProviderType): Boolean {
        return providerType == ProviderType.NAVER
    }
}