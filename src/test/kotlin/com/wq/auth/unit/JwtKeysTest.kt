package com.wq.auth.unit

import com.wq.auth.security.jwt.JwtKeys
import com.wq.auth.support.TestJwtKeys
import io.jsonwebtoken.security.Jwks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class JwtKeysTest : StringSpec({

    "개인키에서 공개키를 유도한다 — modulus 가 같다" {
        val keys = TestJwtKeys.keys()
        keys.publicKey.modulus shouldBe keys.privateKey.let { (it as java.security.interfaces.RSAPrivateCrtKey).modulus }
        keys.publicKey.algorithm shouldBe "RSA"
    }

    "keyId 는 RFC 7638 썸프린트다" {
        val keys = TestJwtKeys.keys()
        val expected = Jwks.builder().key(keys.publicKey).idFromThumbprint().build().id
        keys.keyId shouldBe expected
        keys.keyId.shouldNotBeBlank()
    }

    "publicJwk 는 공개 파라미터만 담는다" {
        val jwk = TestJwtKeys.keys().publicJwk
        jwk["kty"] shouldBe "RSA"
        jwk["use"] shouldBe "sig"
        jwk["alg"] shouldBe "RS256"
        jwk["kid"] shouldBe TestJwtKeys.keys().keyId
        jwk["e"] shouldBe "AQAB"
        (jwk["n"] as String).shouldNotBeBlank()
        jwk.containsKey("d") shouldBe false
        jwk.containsKey("p") shouldBe false
        jwk.containsKey("q") shouldBe false
    }

    "레거시 시크릿이 비어 있으면 legacySecretKey 는 null" {
        TestJwtKeys.keys(withLegacy = false).legacySecretKey.shouldBeNull()
    }

    "레거시 시크릿이 있으면 HMAC 키를 만든다" {
        val key = TestJwtKeys.keys(withLegacy = true).legacySecretKey
        key.shouldNotBeNull()
        key.algorithm shouldBe "HmacSHA256"
    }

    "개인키가 비어 있으면 명확한 예외" {
        val ex = shouldThrow<IllegalArgumentException> { JwtKeys("") }
        ex.message shouldBe "jwt.private-key 가 비어 있습니다. PKCS#8 개인키를 한 줄 base64 로 넣으십시오."
    }

    "PKCS#8 이 아닌 값이면 예외" {
        shouldThrow<IllegalArgumentException> {
            JwtKeys(java.util.Base64.getEncoder().encodeToString("not-a-key".toByteArray()))
        }
    }
})
