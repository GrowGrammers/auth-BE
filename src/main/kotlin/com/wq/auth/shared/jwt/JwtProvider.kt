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

    fun createAccessTokenDeprecated(
        subject: String
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.accessExp))

        return Jwts.builder()
            .issuedAt(Date.from(now))
            .expiration(exp)
            .claim("subject", subject)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

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

    fun createRefreshTokenDeprecated(
        subject: String,
        jti: String = UUID.randomUUID().toString()
    ): Pair<String, String> { // Pair<token, jti>
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.refreshExp))

        val token = Jwts.builder()
            .subject(subject)
            .id(jti)                 // jti 클레임: RefreshToken 고유 식별자
            .issuedAt(Date.from(now))
            .expiration(exp)
            .signWith(key, Jwts.SIG.HS256)
            .compact()

        return Pair(token, jti)
    }

    fun getOpaqueId(token: String): String =
        Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload
            .subject

    fun getRoleDeprecated(token: String): String? =
        Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload
            .get("role", String::class.java)

    fun getRole(token: String): Role? {
        val roleString = Jwts.parser().verifyWith(key)
            .build().parseSignedClaims(token)
            .payload
            .get("role", String::class.java)

        return roleString?.let { Role.valueOf(it) }
    }

    fun getSubject(token: String): String =
        Jwts.parser().verifyWith(key)                   // 서명 검증에 사용할 키 설정
            .build().parseSignedClaims(token)     // 토큰을 검증하며 파싱
            .payload                                    // Claims 객체 추출
            .subject

    //jti 반환
    fun getJti(token: String): String =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload.id


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