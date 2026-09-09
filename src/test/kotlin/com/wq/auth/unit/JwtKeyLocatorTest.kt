package com.wq.auth.unit

import com.wq.auth.security.jwt.JwtKeyLocator
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.wq.auth.support.TestJwtKeys
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * 로케이터는 파서 안에서 호출되므로 파서를 통해 검증한다.
 * 토큰은 jjwt 로 직접 만든다 — JwtProvider 는 이 시점에 아직 RS256 을 모른다.
 */
class JwtKeyLocatorTest : StringSpec({

    val keysWithLegacy = TestJwtKeys.keys(withLegacy = true)
    val keysNoLegacy = TestJwtKeys.keys(withLegacy = false)

    fun parserFor(keys: com.wq.auth.security.jwt.JwtKeys) =
        Jwts.parser().keyLocator(JwtKeyLocator(keys)).build()

    fun rs256(keys: com.wq.auth.security.jwt.JwtKeys, kid: String = keys.keyId): String =
        Jwts.builder().header().keyId(kid).and()
            .subject("user-1")
            .signWith(keys.privateKey, Jwts.SIG.RS256)
            .compact()

    fun hs256Legacy(): String =
        Jwts.builder().subject("user-1")
            .signWith(keysWithLegacy.legacySecretKey!!, Jwts.SIG.HS256)
            .compact()

    "RS256 + 현재 kid 는 공개키로 검증된다" {
        parserFor(keysNoLegacy).parseSignedClaims(rs256(keysNoLegacy)).payload.subject shouldBe "user-1"
    }

    "RS256 인데 kid 가 다르면 INVALID_SIGNATURE" {
        val ex = shouldThrow<JwtException> {
            parserFor(keysNoLegacy).parseSignedClaims(rs256(keysNoLegacy, kid = "unknown-kid"))
        }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "RS256 인데 kid 가 없으면 INVALID_SIGNATURE" {
        val token = Jwts.builder().subject("user-1")
            .signWith(keysNoLegacy.privateKey, Jwts.SIG.RS256).compact()
        val ex = shouldThrow<JwtException> { parserFor(keysNoLegacy).parseSignedClaims(token) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "HS256 은 레거시 시크릿이 있으면 통과한다" {
        parserFor(keysWithLegacy).parseSignedClaims(hs256Legacy()).payload.subject shouldBe "user-1"
    }

    "HS256 은 레거시 시크릿이 없으면 INVALID_SIGNATURE" {
        val ex = shouldThrow<JwtException> { parserFor(keysNoLegacy).parseSignedClaims(hs256Legacy()) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "알고리즘 혼동 — 공개키 바이트를 HMAC 시크릿으로 쓴 HS256 토큰은 거부된다 (시크릿 없음)" {
        val forged = Jwts.builder().header().keyId(keysNoLegacy.keyId).and()
            .subject("attacker")
            .signWith(Keys.hmacShaKeyFor(keysNoLegacy.publicKey.encoded), Jwts.SIG.HS256)
            .compact()
        val ex = shouldThrow<JwtException> { parserFor(keysNoLegacy).parseSignedClaims(forged) }
        ex.jwtCode shouldBe JwtExceptionCode.INVALID_SIGNATURE
    }

    "알고리즘 혼동 — 시크릿이 있어도 공개키 바이트 HS256 토큰은 서명 불일치로 거부된다" {
        val forged = Jwts.builder().header().keyId(keysWithLegacy.keyId).and()
            .subject("attacker")
            .signWith(Keys.hmacShaKeyFor(keysWithLegacy.publicKey.encoded), Jwts.SIG.HS256)
            .compact()
        // 레거시 시크릿으로 검증하다 서명 불일치 → jjwt SignatureException
        shouldThrow<io.jsonwebtoken.security.SignatureException> {
            parserFor(keysWithLegacy).parseSignedClaims(forged)
        }
    }
})
