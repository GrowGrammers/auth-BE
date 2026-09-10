package com.wq.auth.security.jwt

import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.github.f4b6a3.uuid.UuidCreator
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import io.jsonwebtoken.security.SecurityException as JjwtSecurityException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
    private val jwtKeys: JwtKeys,
) {
    /**
     * 검증 키는 헤더를 보고 [JwtKeyLocator] 가 고른다 — RS256 은 공개키, HS256 은 레거시 시크릿(있을 때만).
     * JwtParser 는 스레드 안전하므로 한 번 만들어 재사용한다.
     */
    private val parser: JwtParser = Jwts.parser()
        .keyLocator(JwtKeyLocator(jwtKeys))
        .build()

    fun createAccessToken(
        opaqueId: String,
        extraClaims: Map<String, Any?> = emptyMap()
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.accessExp))

        return Jwts.builder()
            .header().keyId(jwtKeys.keyId).and()
            .subject(opaqueId)
            .issuedAt(Date.from(now))
            .expiration(exp)
            .apply {
                extraClaims.forEach { (key, value) ->
                    claim(key, value)
                }
            }
            .signWith(jwtKeys.privateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun createRefreshToken(
        opaqueId: String,
        jti: String = UuidCreator.getTimeOrdered().toString()
    ): String {
        val now = Instant.now()
        val exp = Date.from(now.plus(jwtProperties.refreshExp))

        return Jwts.builder()
            .header().keyId(jwtKeys.keyId).and()
            .subject(opaqueId)
            .id(jti)                 // jti 클레임: RefreshToken 고유 식별자
            .issuedAt(Date.from(now))
            .expiration(exp)
            .signWith(jwtKeys.privateKey, Jwts.SIG.RS256)
            .compact()
    }

    /**
     * JWT 토큰에서 opaqueId(subject)를 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 사용자의 opaqueId (UUID)
     */
    fun getOpaqueId(token: String): String =
        parser.parseSignedClaims(token)
            .payload
            .subject

    /**
     * JWT 토큰에서 jti(ID)를 추출합니다.
     * @param token 대상 JWT 토큰
     * @return JWT ID (RefreshToken 고유 식별자)
     */
    fun getJti(token: String): String =
        parser.parseSignedClaims(token)
            .payload
            .id

    /**
     * JWT 토큰에서 발급 시각(iat)을 추출합니다.
     *
     * iat 는 JWT 표준상 **초 단위** 정밀도입니다. 폐기 판정에서 이 점이 중요합니다 —
     * 같은 초에 발급된 토큰까지 거부하려면 호출부가 `iat > invalidBefore` 가 아니라
     * 그 부정(`!isAfter`)으로 비교해야 합니다.
     *
     * 서명을 검증하므로 위조된 토큰이면 예외를 던집니다.
     *
     * @param token 대상 JWT 토큰
     * @return 발급 시각
     */
    fun getIssuedAt(token: String): Instant =
        parser.parseSignedClaims(token)
            .payload
            .issuedAt.toInstant()

    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     * @param token 대상 JWT 토큰
     * @return 모든 클레임을 담은 Map
     */
    fun getAllClaims(token: String): Map<String, Any> =
        parser.parseSignedClaims(token)
            .payload

    /**
     * 액세스 토큰의 만료 시간(초)을 반환합니다.
     * @return 액세스 토큰 만료 시간 (초 단위)
     */
    fun getAccessTokenExpirationSeconds(): Long = jwtProperties.accessExp.toSeconds()

    /**
     * RefreshToken의 만료 시각을 반환합니다.
     */
    fun getRefreshTokenExpiredAt(token: String): Instant =
        parser.parseSignedClaims(token)
            .payload
            .expiration.toInstant()

    /**
     * 토큰의 남은 유효 시간을 초 단위로 반환합니다.
     *
     * - 양수: 아직 유효하며 해당 초만큼 남음
     * - 음수(-1): 이미 만료된 토큰 (서명 자체는 유효)
     * - 서명 오류, 위조 등 구조적으로 유효하지 않은 토큰은 [JwtException] 을 던집니다.
     */
    fun getRemainingTimeSeconds(token: String): Long {
        return try {
            val expiration = parser.parseSignedClaims(token).payload.expiration
            (expiration.time - System.currentTimeMillis()) / 1000
        } catch (e: ExpiredJwtException) {
            -1L
        } catch (t: Throwable) {
            throw JwtException(mapToCode(t), t)
        }
    }

    /**
     * 만료 여부와 관계없이 토큰의 클레임을 추출합니다.
     *
     * 만료된 토큰이라도 서명이 유효하다면 클레임을 반환합니다.
     * 이는 사일런트 리프레시 시 만료된 AT에서 opaqueId/deviceId를 읽어야 할 때 사용합니다.
     * 서명이 위조되거나 형식이 잘못된 경우에는 [JwtException] 을 던집니다.
     */
    fun getClaimsEvenIfExpired(token: String): Claims {
        return try {
            parser.parseSignedClaims(token).payload
        } catch (e: ExpiredJwtException) {
            e.claims
        } catch (t: Throwable) {
            throw JwtException(mapToCode(t), t)
        }
    }

    /**
     * 유효성 검사(예외 던짐) – 표준 에러로 변환
     * 컨트롤러/서비스에서 이 메서드를 사용하면 GlobalExceptionHandler가 잡아줍니다.
     */
    fun validateOrThrow(token: String) {
        try {
            parser.parseSignedClaims(token)
        } catch (throwable: Throwable) {
            throw JwtException(mapToCode(throwable), throwable)
        }
    }

    /**
     * jjwt 예외를 도메인 코드로 옮긴다.
     *
     * 타입을 정확히 짚는 것이 중요하다 —
     * - `JwtException` 은 도메인 예외 [com.wq.auth.security.jwt.error.JwtException] 이다(jjwt 의 동명 타입이 아니다).
     * - [JjwtSecurityException] 은 `io.jsonwebtoken.security.SecurityException` 이다. 별칭 없이 `SecurityException`
     *   이라고 쓰면 `java.lang.SecurityException` 이 잡혀 서명 오류가 MALFORMED 로 흘러간다.
     * - jjwt 의 [SignatureException] 은 [JjwtSecurityException] 의 하위 타입이므로 한 분기로 합쳐 둔다.
     */
    private fun mapToCode(throwable: Throwable): JwtExceptionCode = when (throwable) {
        is JwtException                     -> throwable.jwtCode   // JwtKeyLocator 가 거부한 경우
        is SignatureException,
        is JjwtSecurityException            -> JwtExceptionCode.INVALID_SIGNATURE
        is MalformedJwtException            -> JwtExceptionCode.MALFORMED
        is ExpiredJwtException              -> JwtExceptionCode.EXPIRED
        is UnsupportedJwtException          -> JwtExceptionCode.UNSUPPORTED
        is IllegalArgumentException         -> JwtExceptionCode.TOKEN_MISSING
        else                                -> JwtExceptionCode.MALFORMED
    }
}