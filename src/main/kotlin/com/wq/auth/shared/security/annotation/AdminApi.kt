package com.wq.auth.shared.security.annotation

import org.springframework.security.access.prepost.PreAuthorize

/**
 * 관리자 권한이 필요한 API에 사용하는 어노테이션
 * 
 * 이 어노테이션이 적용된 메서드나 클래스는 ROLE_ADMIN 권한을 가진 사용자만 접근할 수 있습니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @AdminApi
 * @GetMapping("/admin/users")
 * fun getAllUsers(): List<User> {
 *     // 관리자만 접근 가능한 로직
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
annotation class AdminApi(
    val description: String = "관리자 권한 필요"
)
