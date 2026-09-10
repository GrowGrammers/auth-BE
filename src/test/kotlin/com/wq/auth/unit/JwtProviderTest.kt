package com.wq.auth.unit

import com.wq.auth.security.jwt.JwtKeys
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.wq.auth.support.TestJwtKeys
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.security.interfaces.RSAPrivateKey
import java.time.Duration
import java.time.Instant
import java.util.Base64

class JwtProviderTest : StringSpec({

    val opaqueId = "550e8400-e29b-41d4-a716-446655440000"

    fun props(
        accessExp: Duration = Duration.ofMinutes(5),
        legacySecret: String = "",
    ) = JwtProperties(
        privateKey = TestJwtKeys.PRIVATE_KEY_B64,
        legacySecret = legacySecret,
        accessExp = accessExp,
        refreshExp = Duration.ofDays(14),
    )

    fun providerOf(p: JwtProperties) = JwtProvider(p, JwtKeys(p.privateKey, p.legacySecret))

    val keys = TestJwtKeys.keys()
    val provider = providerOf(props())

    /** 다른 RSA 키쌍 — 위조 시나리오용 */
    fun anotherKeys(): JwtKeys {
        val pair = Jwts.SIG.RS256.keyPair().build()
        val pkcs8 = Base64.getEncoder().encodeToString((pair.private as RSAPrivateKey).encoded)
        return JwtKeys(pkcs8)
    }

    "AccessToken 은 RS256 으로 서명되고 헤더에 현재 kid 가 있다" {
        val token = provider.createAccessToken(opaqueId)
        val jws = Jwts.parser().verifyWith(keys.publicKey).build().parseSignedClaims(token)
        jws.header.algorithm shouldBe "RS256"
        jws.header.keyId shouldBe keys.keyId
        jws.payload.subject shouldBe opaqueId
    }

    "RefreshToken 도 RS256 으로 서명되고 jti 가 있다" {
        val rt = provider.createRefreshToken(opaqueId)
        val jws = Jwts.parser().verifyWith(keys.publicKey).build().parseSignedClaims(rt)
        jws.header.algorithm shouldBe "RS256"
        jws.header.keyId shouldBe keys.keyId
        (jws.payload.id?.isNotBlank() ?: false).shouldBeTrue()
        provider.getJti(rt) shouldBe jws.payload.id
    }

    "AccessToken 에 extraClaims 가 들어간다" {
        val token = provider.createAccessToken(opaqueId, mapOf("role" to "ADMIN"))
        val claims = Jwts.parser().verifyWith(keys.publicKey).build().parseSignedClaims(token).payload
        claims["role"] shouldBe "ADMIN"
        claims.subject shouldBe opaqueId
        claims.issuedAt shouldNotBe null
        claims.expiration shouldNotBe null
    }

    "발급한 토큰에서 opaqueId 를 읽는다" {
        provider.getOpaqueId(provider.createAccessToken(opaqueId)) shouldBe opaqueId
        provider.getOpaqueId(provider.createRefreshToken(opaqueId)) shouldBe opaqueId
    }

    "RS256 토큰은 validateOrThrow 를 통과한다" {
        provider.validateOrThrow(provider.createAccessToken(opaqueId))
    }

    "다른 키쌍으로 서명한 토큰은 INVALID_SIGNATURE" {
        val other = anotherKeys()
        val tokenByOther = Jwts.builder().header().keyId(other.keyId).and()
            .subject(opaqueId).signWith(other.privateKey, Jwts.SIG.RS256).compact()
        val ex = shouldThrow<JwtException> { provider.validateOrThrow(tokenByOther) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "현재 kid 를 달았지만 다른 개인키로 서명한 토큰은 INVALID_SIGNATURE" {
        val other = anotherKeys()
        val forged = Jwts.builder().header().keyId(keys.keyId).and()
            .subject(opaqueId).signWith(other.privateKey, Jwts.SIG.RS256).compact()
        val ex = shouldThrow<JwtException> { provider.validateOrThrow(forged) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "레거시 시크릿이 있으면 HS256 토큰을 받아들인다 (전환 기간)" {
        val legacyProvider = providerOf(props(legacySecret = TestJwtKeys.LEGACY_SECRET_B64))
        val legacyKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TestJwtKeys.LEGACY_SECRET_B64))
        val hs256 = Jwts.builder().subject(opaqueId)
            .issuedAt(java.util.Date()).expiration(java.util.Date(System.currentTimeMillis() + 60_000))
            .signWith(legacyKey, Jwts.SIG.HS256).compact()

        legacyProvider.validateOrThrow(hs256)
        legacyProvider.getOpaqueId(hs256) shouldBe opaqueId
    }

    "레거시 시크릿이 없으면 같은 HS256 토큰을 INVALID_SIGNATURE 로 거부한다" {
        val legacyKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TestJwtKeys.LEGACY_SECRET_B64))
        val hs256 = Jwts.builder().subject(opaqueId)
            .issuedAt(java.util.Date()).expiration(java.util.Date(System.currentTimeMillis() + 60_000))
            .signWith(legacyKey, Jwts.SIG.HS256).compact()

        val ex = shouldThrow<JwtException> { provider.validateOrThrow(hs256) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "알고리즘 혼동 — 공개키 바이트로 HS256 서명한 토큰은 INVALID_SIGNATURE" {
        val forged = Jwts.builder().header().keyId(keys.keyId).and()
            .subject("attacker")
            .signWith(Keys.hmacShaKeyFor(keys.publicKey.encoded), Jwts.SIG.HS256).compact()
        val ex = shouldThrow<JwtException> { provider.validateOrThrow(forged) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "만료된 토큰은 EXPIRED" {
        val shortProvider = providerOf(props(accessExp = Duration.ofMillis(100)))
        val token = shortProvider.createAccessToken(opaqueId)
        Thread.sleep(200)
        val ex = shouldThrow<JwtException> { shortProvider.validateOrThrow(token) }
        ex.jwtCode shouldBe JwtExceptionCode.EXPIRED
    }

    "만료된 토큰도 getClaimsEvenIfExpired 로 클레임을 읽는다" {
        val shortProvider = providerOf(props(accessExp = Duration.ofMillis(100)))
        val token = shortProvider.createAccessToken(opaqueId, mapOf("deviceId" to "dev-1"))
        Thread.sleep(200)
        val claims = shortProvider.getClaimsEvenIfExpired(token)
        claims.subject shouldBe opaqueId
        claims["deviceId"] shouldBe "dev-1"
    }

    "getRemainingTimeSeconds 는 유효하면 양수, 만료면 -1" {
        val remaining = provider.getRemainingTimeSeconds(provider.createAccessToken(opaqueId))
        (remaining in 1..300).shouldBeTrue()

        val shortProvider = providerOf(props(accessExp = Duration.ofMillis(100)))
        val token = shortProvider.createAccessToken(opaqueId)
        Thread.sleep(200)
        shortProvider.getRemainingTimeSeconds(token) shouldBe -1L
    }

    "세그먼트가 2개면 MALFORMED" {
        val ex = shouldThrow<JwtException> { provider.validateOrThrow("abc.def") }
        ex.jwtCode shouldBe JwtExceptionCode.MALFORMED
    }

    "alg=none 토큰은 UNSUPPORTED" {
        val headerB64 = Encoders.BASE64URL.encode("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payloadB64 = Encoders.BASE64URL.encode("""{"sub":"user-123"}""".toByteArray())
        val sigB64 = Encoders.BASE64URL.encode("sig".toByteArray())
        // parseSignedClaims 는 unsecured JWS 를 UnsupportedJwtException 으로 확정적으로 거부한다.
        val ex = shouldThrow<JwtException> { provider.validateOrThrow("$headerB64.$payloadB64.$sigB64") }
        ex.jwtCode shouldBe JwtExceptionCode.UNSUPPORTED
    }

    "빈 토큰 문자열이면 TOKEN_MISSING" {
        val ex = shouldThrow<JwtException> { provider.validateOrThrow("") }
        ex.jwtCode shouldBe JwtExceptionCode.TOKEN_MISSING
    }

    "getIssuedAt() 은 발급 시각을 초 단위로 돌려준다" {
        val before = Instant.now().minusSeconds(2)
        val token = provider.createAccessToken(opaqueId)
        val after = Instant.now().plusSeconds(2)
        val issuedAt = provider.getIssuedAt(token)
        (issuedAt.isAfter(before) && issuedAt.isBefore(after)).shouldBeTrue()
        issuedAt.nano shouldBe 0
    }

    "getIssuedAt() 은 서명이 위조된 토큰이면 예외" {
        val forged = provider.createAccessToken(opaqueId).dropLast(4) + "AAAA"
        shouldThrow<Exception> { provider.getIssuedAt(forged) }
    }

    "getRefreshTokenExpiredAt 은 refreshExp 뒤 시각이다" {
        val rt = provider.createRefreshToken(opaqueId)
        val exp = provider.getRefreshTokenExpiredAt(rt)
        exp.isAfter(Instant.now().plus(Duration.ofDays(13))).shouldBeTrue()
    }

    "getAccessTokenExpirationSeconds 는 accessExp 초" {
        provider.getAccessTokenExpirationSeconds() shouldBe 300L
    }
})
