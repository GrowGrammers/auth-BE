package com.wq.auth.shared.config

import com.wq.auth.shared.security.JwtAccessDeniedHandler
import com.wq.auth.shared.security.JwtAuthenticationFilter
import com.wq.auth.shared.security.JwtAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security 설정 클래스
 * 
 * JWT 기반 인증을 위한 보안 설정을 구성합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            // CSRF 보호 비활성화 (JWT 사용 시 불필요)
            .csrf { it.disable() }
            
            // Form Login 비활성화 (JWT 기반 API이므로 불필요)
            .formLogin { it.disable() }
            
            // HTTP Basic 인증 비활성화
            .httpBasic { it.disable() }
            
            // 세션 정책: STATELESS (JWT 사용으로 세션 불필요)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            
            // 요청 권한 설정
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트 (인증 불필요)
                    .requestMatchers(
                        "/api/public/**",         // 공개 API
                        "/actuator/health"        // 헬스체크
                    ).permitAll()
                    
                    // 나머지 모든 요청은 인증 필요 (세부 권한은 @PreAuthorize로 처리)
                    .anyRequest().authenticated()
            }
            
            // H2 콘솔을 위한 프레임 옵션 설정
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            
            // 예외 처리 설정
            .exceptionHandling { exceptions ->
                exceptions
                    // 401 Unauthorized: 인증 실패 (토큰 없음/잘못됨)
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    // 403 Forbidden: 인가 실패 (권한 부족)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
            }
            
            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            
            .build()
    }
}
