package com.wq.auth.domain.auth

import com.wq.auth.api.domain.auth.LoginProvider
import com.wq.auth.domain.auth.request.SocialLoginRequest
import com.wq.auth.domain.auth.response.SocialLoginResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val loginProviders: List<LoginProvider>,
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

        return loginProviders.find{it.support(request.providerType) }
            ?.processLogin(request)
            ?:throw Exception()
    }
}
