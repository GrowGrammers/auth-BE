package com.wq.demo.unit

import com.wq.demo.jwt.JwtProperties
import com.wq.demo.jwt.JwtProvider
import com.wq.demo.jwt.error.JwtException
import com.wq.demo.jwt.error.JwtExceptionCode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.Base64
import javax.crypto.SecretKey

class JwtProviderTest : StringSpec({

    // 256-bit(32byte) 시크릿을 Base64로 준비
    val plain = "01234567890123456789012345678901" // 32 chars = 256-bit (ASCII)
    val secretB64 = Base64.getEncoder().encodeToString(plain.toByteArray())

    val props = JwtProperties(
        secret = secretB64,
        accessExp = Duration.ofSeconds(1),   // 만료 테스트를 위해 짧게
        refreshExp = Duration.ofDays(14)
    )
    val provider = JwtProvider(props)

    "AccessToken을 발급하면 subject 파싱이 정상 동작한다" {
        val token = provider.createAccessToken(
            subject = "user-123",
            extraClaims = mapOf("role" to "USER")
        )
        provider.getSubject(token) shouldBe "user-123"
    }

    "AccessToken에 넣은 커스텀 claim(role)이 실제로 들어간다" {
        val token = provider.createAccessToken(
            subject = "user-123",
            extraClaims = mapOf("role" to "USER")
        )
        val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret))
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        claims["role"] shouldBe "USER"
        claims.subject shouldBe "user-123"
    }

    "위조된 토큰은 JwtException(INVALID_SIGNATURE)을 던진다" {
        fun b64(s: String) = Base64.getEncoder().encodeToString(s.toByteArray())
        val keyB = b64("abcdefghijklmnopqrstuvwxyzABCDEF") // 다른 키(32바이트)
        val providerWithAnotherKey = JwtProvider(
            JwtProperties(secret = keyB, accessExp = Duration.ofMinutes(5), refreshExp = Duration.ofDays(14))
        )
        val tokenSignedByB = providerWithAnotherKey.createAccessToken("user-123")

        val ex = shouldThrow<JwtException> {
            provider.validateOrThrow(tokenSignedByB)
        }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "만료된 토큰은 JwtException(EXPIRED)을 던진다" {
        val token = provider.createAccessToken("user-123")
        Thread.sleep(1_200) // accessExp=1s 대기
        val ex = shouldThrow<JwtException> { provider.validateOrThrow(token) }
        ex.jwtCode shouldBe JwtExceptionCode.EXPIRED
    }

    "RefreshToken을 발급하면 subject 파싱이 정상 동작한다" {
        val rt = provider.createRefreshToken("user-123")
        provider.getSubject(rt) shouldBe "user-123"
    }

    "RefreshToken에는 jti가 포함된다" {
        val rt = provider.createRefreshToken("user-123")
        val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret))
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(rt).payload
        (claims.id?.isNotBlank() ?: false).shouldBeTrue()
    }

    "세그먼트가 2개면 MALFORMED를 던진다" {
        val malformed = "abc.def" // 3개가 아닌 compact JWS
        val ex = shouldThrow<JwtException> { provider.validateOrThrow(malformed) }
        ex.jwtCode shouldBe JwtExceptionCode.MALFORMED
    }

    "alg=none 토큰은 UNSUPPORTED를 던진다" {
        val headerJson = """{"alg":"none","typ":"JWT"}"""
        val payloadJson = """{"sub":"user-123"}"""
        val headerB64 = Encoders.BASE64URL.encode(headerJson.toByteArray())
        val payloadB64 = Encoders.BASE64URL.encode(payloadJson.toByteArray())
        val signatureB64 = Encoders.BASE64URL.encode("sig".toByteArray())

        val unsupported = "$headerB64.$payloadB64.$signatureB64"

        val ex = shouldThrow<JwtException> { provider.validateOrThrow(unsupported) }
        ex.jwtCode shouldBe JwtExceptionCode.UNSUPPORTED
    }

    "빈 토큰 문자열이면 TOKEN_MISSING을 던진다" {
        val ex = shouldThrow<JwtException> { provider.validateOrThrow("") }
        ex.jwtCode shouldBe JwtExceptionCode.TOKEN_MISSING
    }
})