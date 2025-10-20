package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.request.OAuthAuthCodeRequest
import com.wq.auth.api.domain.auth.request.SocialLinkRequest
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.external.oauth.KakaoOAuthClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 카카오 계정 연동 Provider
 */
@Component
class KakaoLinkProvider(
    private val kakaoOAuthClient: KakaoOAuthClient,
    authProviderRepository: AuthProviderRepository,
    memberRepository: MemberRepository,
) : AbstractLinkProvider(authProviderRepository, memberRepository) {

    private val log = KotlinLogging.logger {}

    override fun processLink(currentMember: MemberEntity, linkRequest: SocialLinkRequest) {
        log.info { "카카오 계정 연동 처리 시작: ${currentMember.opaqueId}" }

        // 카카오 OAuth2로부터 사용자 정보 조회
        val authCodeRequest = OAuthAuthCodeRequest(
            authCode = linkRequest.authCode,
            codeVerifier = linkRequest.codeVerifier,
        )
        val oauthUser = kakaoOAuthClient.getUserFromAuthCode(authCodeRequest)

        // 계정 연동 처리
        linkAccount(currentMember, oauthUser, ProviderType.KAKAO)

        log.info { "카카오 계정 연동 완료: ${currentMember.opaqueId}" }
    }

    override fun support(providerType: ProviderType): Boolean {
        return providerType == ProviderType.KAKAO
    }
}
