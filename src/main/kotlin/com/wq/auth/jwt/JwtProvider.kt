package com.wq.auth.jwt

import com.wq.auth.jwt.error.JwtException
import com.wq.auth.jwt.error.JwtExceptionCode
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
     * AccessToken을 발급.
     *
     * @param subject 토큰 주체 (opaque: 랜덤 ulid)
     * @param extraClaims 토큰에 추가로 넣을 Claim (role, 권한 등)
     * @return 생성된 JWT AccessToken 문자열
     *
     * 기본 클레임:
     * - sub : subject
     * - iat : 발급 시각
     * - exp : 만료 시각
     *
     * extraClaims로 추가적인 정보를 담을 수 있습니다.
     */
    fun createAccessToken(
        subject: String,
        extraClaims: Map<String, Any?> = emptyMap()
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.accessExp))

        val jwtBuilder = Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(exp)

        // null 값은 claim에 포함하지 않음
        extraClaims.filterValues { it != null }
            .forEach { (key, value) -> jwtBuilder.claim(key, value) }

        return jwtBuilder.signWith(key, Jwts.SIG.HS256).compact()
    }

    /**
     * RefreshToken을 발급.
     *
     * @param subject 토큰 주체 (opaque: 랜덤 ulid)
     * @param jti 토큰 고유 식별자 (재발급/회전 시 검증용, 기본값: 랜덤 UUID)
     * @return 생성된 JWT RefreshToken 문자열
     *
     * DB에 jti를 저장해두고 재사용 방지 로직을 구현.
     */
    fun createRefreshToken(
        subject: String,
        jti: String = UUID.randomUUID().toString()
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.refreshExp))

        return Jwts.builder()
            .subject(subject)
            .id(jti)                 // jti 클레임: RefreshToken 고유 식별자
            .issuedAt(Date.from(now))
            .expiration(exp)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * JWT 토큰에서 주체(subject)를 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 토큰의 주체 (사용자 ulid/uuid)
     */
    fun getSubject(token: String): String =
        Jwts.parser().verifyWith(key)                   // 서명 검증에 사용할 키 설정
            .build().parseSignedClaims(token)     // 토큰을 검증하며 파싱
            .payload                                    // Claims 객체 추출
            .subject                                    // 주체 값 반환


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