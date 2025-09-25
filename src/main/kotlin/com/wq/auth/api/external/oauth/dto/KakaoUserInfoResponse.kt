package com.wq.auth.api.external.oauth.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 카카오 OAuth2 사용자 정보 응답 DTO
 * 
 * 카카오 UserInfo API (/v2/user/me)의 응답을 파싱하기 위한 데이터 클래스입니다.
 * 
 * 카카오 API 응답 구조:
 * {
 *   "id": 1234567890,
 *   "kakao_account": {
 *     "email": "user@example.com",
 *     "email_needs_agreement": false,
 *     "is_email_valid": true,
 *     "is_email_verified": true,
 *     "profile": {
 *       "nickname": "홍길동",
 *       "thumbnail_image_url": "...",
 *       "profile_image_url": "..."
 *     }
 *   }
 * }
 */
data class KakaoUserInfoResponse(
    @field:JsonProperty("id")
    val id: Long,

    @field:JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount
) {
    /**
     * 닉네임을 생성합니다.
     * 우선순위: profile.nickname -> email의 @ 앞부분
     */
    fun getNickname(): String {
        return kakaoAccount.profile?.nickname?.takeIf { it.isNotBlank() }
            ?: kakaoAccount.email?.substringBefore("@") 
            ?: "카카오사용자$id"
    }

    /**
     * 카카오 제공자 ID를 반환합니다.
     */
    fun getProviderId(): String = id.toString()

    /**
     * 이메일을 반환합니다.
     */
    fun getEmail(): String = kakaoAccount.email ?: ""

    /**
     * 이메일 검증 여부를 반환합니다.
     */
    fun isEmailVerified(): Boolean = kakaoAccount.isEmailVerified ?: false
}

data class KakaoAccount(
    @field:JsonProperty("email")
    val email: String? = null,

    @field:JsonProperty("email_needs_agreement")
    val emailNeedsAgreement: Boolean = false,

    @field:JsonProperty("is_email_valid")
    val isEmailValid: Boolean = false,

    @field:JsonProperty("is_email_verified")
    val isEmailVerified: Boolean? = false,

    @field:JsonProperty("profile")
    val profile: KakaoProfile? = null
)

data class KakaoProfile(
    @field:JsonProperty("nickname")
    val nickname: String? = null,

    @field:JsonProperty("thumbnail_image_url")
    val thumbnailImageUrl: String? = null,

    @field:JsonProperty("profile_image_url")
    val profileImageUrl: String? = null
)
