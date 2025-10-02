package com.wq.auth.api.domain.auth

import com.wq.auth.api.domain.auth.entity.AuthProviderEntity
import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.request.SocialLoginRequest
import com.wq.auth.api.domain.auth.response.SocialLoginResult
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.oauth.OAuthUser
import io.github.oshai.kotlinlogging.KotlinLogging

interface LoginProvider {
    fun processLogin(loginRequest: SocialLoginRequest): SocialLoginResult
    fun support(providerType: ProviderType): Boolean
}

abstract class AbstractLoginProvider(
    private val authProviderRepository: AuthProviderRepository,
    private val memberRepository: MemberRepository,
) : LoginProvider {
    private val log = KotlinLogging.logger {}
    /**
     * 기존 회원을 찾거나 신규 회원을 생성합니다.
     *
     * @param oauthUser OAuth 사용자 정보
     * @param providerType 소셜 제공자 타입
     * @return Pair<회원 엔티티, 신규 회원 여부>
     */
    fun findOrCreateMember(
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
    fun createOrUpdateAuthProvider(
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