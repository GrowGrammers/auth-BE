package com.wq.auth.shared.security.annotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * 인증된 사용자만 접근할 수 있는 API에 사용하는 어노테이션
 * 
 * 이 어노테이션이 적용된 메서드나 클래스는 유효한 JWT 토큰을 가진 사용자만 접근할 수 있습니다.
 * 역할(ROLE)에 상관없이 인증된 모든 사용자가 접근 가능합니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @AuthenticatedApi
 * @GetMapping("/api/user/profile")
 * fun getProfile(): UserProfile {
 *     // 인증된 사용자만 접근 가능한 로직
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("isAuthenticated()")
annotation class AuthenticatedApi(
    val description: String = "인증 필요"
)
