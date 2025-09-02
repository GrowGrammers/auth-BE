package com.wq.auth.shared.jwt

import com.wq.auth.api.domain.member.entity.Role
import com.wq.auth.shared.jwt.error.JwtException
import com.wq.auth.shared.jwt.error.JwtExceptionCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(
        Decoders.BASE64.decode(jwtProperties.secret)
    )

    /**
     * AccessToken을 발급합니다.
     *
     * @param opaqueId 사용자의 UUID (opaque identifier)
     * @param role 사용자 역할 (MEMBER, ADMIN)
     * @return 생성된 JWT AccessToken 문자열
     *
     * 토큰 구조:
     * - sub : opaqueId (UUID)
     * - iat : 발급 시각
     * - exp : 만료 시각
     * - role : 사용자 역할
     */
    fun createAccessToken(
        opaqueId: String,
        role: Role
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.accessExp))

        return Jwts.builder()
            .subject(opaqueId)
            .issuedAt(Date.from(now))
            .expiration(exp)
            .claim("role", role.toString())
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * RefreshToken을 발급합니다.
     *
     * @param opaqueId 사용자의 UUID (opaque identifier)
     * @param jti 토큰 고유 식별자 (재발급/회전 시 검증용, 기본값: 랜덤 UUID)
     * @return 생성된 JWT RefreshToken 문자열
     *
     * RefreshToken은 role 정보가 필요하지 않으므로 opaqueId와 만료시간만 포함합니다.
     * DB에 jti를 저장해두고 재사용 방지 로직을 구현할 수 있습니다.
     */
    fun createRefreshToken(
        opaqueId: String,
        jti: String = UUID.randomUUID().toString()
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.refreshExp))

        return Jwts.builder()
            .subject(opaqueId)
            .id(jti)                 // jti 클레임: RefreshToken 고유 식별자
            .issuedAt(Date.from(now))
            .expiration(exp)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * JWT 토큰에서 opaqueId(subject)를 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 사용자의 opaqueId (UUID)
     */
    fun getOpaqueId(token: String): String =
        Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload
            .subject

    /**
     * JWT 토큰에서 role을 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 사용자의 역할 (MEMBER, ADMIN 등)
     */
    fun getRole(token: String): Role? {
        val roleString = Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload
            .get("role", String::class.java)
        
        return roleString?.let { Role.valueOf(it) }
    }

    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 모든 클레임을 담은 Map
     */
    fun getAllClaims(token: String): Map<String, Any> =
        Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload

    /**
     * 유효성 검사(예외 던짐) – 표준 에러로 변환
     * 컨트롤러/서비스에서 이 메서드를 사용하면 GlobalExceptionHandler가 잡아줍니다.
     */
    fun validateOrThrow(token: String) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        } catch (throwable: Throwable) {
            throw JwtException(mapToCode(throwable), throwable)
        }
    }

    private fun mapToCode(throwable: Throwable): JwtExceptionCode = when (throwable) {
        is SignatureException,
        is SecurityException                -> JwtExceptionCode.INVALID_SIGNATURE
        is MalformedJwtException            -> JwtExceptionCode.MALFORMED
        is ExpiredJwtException              -> JwtExceptionCode.EXPIRED
        is UnsupportedJwtException          -> JwtExceptionCode.UNSUPPORTED
        is IllegalArgumentException         -> JwtExceptionCode.TOKEN_MISSING
        else                                -> JwtExceptionCode.MALFORMED
    }
}