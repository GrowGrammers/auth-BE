package com.wq.auth.shared.security.principal

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Spring Security UserDetails 구현체
 * 
 * JWT 토큰에서 추출한 간소화된 사용자 정보를 Spring Security 컨텍스트에서 사용할 수 있도록 변환합니다.
 * 
 * 간소화된 구조:
 * - opaqueId: 사용자 식별을 위한 UUID
 * - role: 사용자 역할 (MEMBER, ADMIN)
 */
data class PrincipalDetails(
    val opaqueId: String,   // 사용자 UUID
    val role: String        // 사용자 역할
) : UserDetails {

    /**
     * 사용자의 권한 목록을 반환합니다.
     * role을 ROLE_ prefix와 함께 GrantedAuthority로 변환합니다.
     */
    override fun getAuthorities(): Collection<GrantedAuthority> {
        val roleWithPrefix = if (role.startsWith("ROLE_")) role else "ROLE_$role"
        return listOf(SimpleGrantedAuthority(roleWithPrefix))
    }

    override fun getPassword(): String? = null

    /**
     * 사용자의 opaqueId 반환.
     */
    override fun getUsername(): String = opaqueId

    override fun isAccountNonExpired(): Boolean = true

    /**
     * 현재 구현에서는 계정 잠금 기능이 없으므로 항상 true를 반환합니다.
     */
    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
