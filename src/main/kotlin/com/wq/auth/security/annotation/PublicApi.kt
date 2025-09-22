package com.wq.auth.security.annotation

/**
 * 인증이 필요하지 않은 공개 API에 사용하는 어노테이션
 * 
 * 이 어노테이션은 문서화 목적으로 사용되며, 실제 보안 설정은 SecurityConfig에서 관리됩니다.
 * 공개 API임을 명시적으로 표현하여 코드의 가독성을 높입니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @PublicApi
 * @PostMapping("/api/auth/login")
 * fun login(@RequestBody loginRequest: LoginRequest): LoginResponse {
 *     // 누구나 접근 가능한 로그인 로직
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PublicApi(
    val description: String = "공개 API"
)
