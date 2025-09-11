package com.wq.auth.security

import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.principal.PrincipalDetails
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * JWT 토큰 기반 인증 필터
 * 
 * HTTP 요청 헤더에서 JWT 토큰을 추출하고 검증하여
 * Spring Security 인증 컨텍스트에 사용자 정보를 설정합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val httpReq = request as HttpServletRequest
        println("Request URI: ${httpReq.requestURI}")

        try {
            // Authorization 헤더에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)
            
            if (token != null) {
                // JWT 토큰 유효성 검증
                jwtProvider.validateOrThrow(token)
                
                // 토큰에서 사용자 정보 추출
                val principalDetails = extractPrincipalDetails(token)
                
                // Spring Security 인증 객체 생성 및 설정
                // todo : TokenService로 분리 필요.
                val authentication = UsernamePasswordAuthenticationToken(
                    principalDetails,       // principal: PrincipalDetails 객체
                    null,                  // credentials: 비밀번호 (JWT에서는 불필요)
                    principalDetails.authorities  // authorities: 사용자 권한
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: JwtException) {
            // JWT 예외는 로깅만 하고 필터 체인을 계속 진행 (인증되지 않은 상태)
            // 인증이 필요한 엔드포인트 접근 시 JwtAuthenticationEntryPoint에서 401 응답 처리
            log.debug(e) { "JWT 인증 실패: ${e.message}" }
        } catch (e: Exception) {
            // 기타 예외는 로깅만 하고 필터 체인을 계속 진행
            log.debug(e) { "JWT 필터 처리 중 예외 발생: ${e.message}" }
        }
        
        // 다음 필터로 진행
        filterChain.doFilter(request, response)
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     * 
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열, 없으면 null
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(AUTHORIZATION_HEADER)
        
        return if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            authorizationHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * JWT 토큰에서 PrincipalDetails 객체를 생성합니다.
     * 
     * @param token JWT 토큰
     * @return PrincipalDetails 객체
     */
    private fun extractPrincipalDetails(token: String): PrincipalDetails {
        val opaqueId = jwtProvider.getOpaqueId(token)
        val role = jwtProvider.getRole(token) ?: Role.MEMBER
        
        return PrincipalDetails(opaqueId, role)
    }
}
