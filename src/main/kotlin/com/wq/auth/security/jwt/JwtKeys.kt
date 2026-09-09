package com.wq.auth.security.jwt

import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Jwks
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.RsaPublicJwk
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.SecretKey

/**
 * JWT 서명·검증에 쓰는 키 묶음. 기동 시 한 번 만들어 재사용한다.
 *
 * - 발급은 RSA 개인키(RS256)로만 한다. 개인키는 auth-api 만 가진다.
 * - 공개키는 개인키에서 유도한다. 별도 설정이 없다.
 * - `kid` 는 RFC 7638 썸프린트다. 키를 바꾸면 저절로 바뀐다.
 * - 레거시 HMAC 키는 **검증 전용**이다. HS256 으로 발급되던 시절의 토큰을
 *   RT 만료 기간 동안 받아 주기 위해서만 존재하며, 비어 있으면 HS256 토큰을 거부한다.
 *
 * 키가 없거나 깨져 있으면 여기서 던져 기동을 막는다. auth-api 는 키 없이 할 일이 없다.
 *
 * @param privateKeyBase64 PKCS#8 PEM 에서 헤더·푸터·개행을 뺀 한 줄 base64
 * @param legacySecretBase64 HS256 시크릿 base64. 비어 있으면 레거시 검증을 끈다
 */
class JwtKeys(
    privateKeyBase64: String,
    legacySecretBase64: String = "",
) {
    val privateKey: RSAPrivateKey
    val publicKey: RSAPublicKey
    val keyId: String
    val publicJwk: RsaPublicJwk
    val legacySecretKey: SecretKey?

    init {
        require(privateKeyBase64.isNotBlank()) {
            "jwt.private-key 가 비어 있습니다. PKCS#8 개인키를 한 줄 base64 로 넣으십시오."
        }
        val crtKey = loadPkcs8(privateKeyBase64)
        privateKey = crtKey
        publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(crtKey.modulus, crtKey.publicExponent)) as RSAPublicKey
        publicJwk = Jwks.builder()
            .key(publicKey)
            .publicKeyUse("sig")
            .algorithm("RS256")
            .idFromThumbprint()
            .build()
        keyId = publicJwk.id
        legacySecretKey = legacySecretBase64.takeIf { it.isNotBlank() }
            ?.let { Keys.hmacShaKeyFor(Decoders.BASE64.decode(it)) }
    }

    private fun loadPkcs8(base64: String): RSAPrivateCrtKey {
        val bytes = try {
            java.util.Base64.getDecoder().decode(base64.trim())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("jwt.private-key 가 올바른 base64 가 아닙니다.", e)
        }
        val key = try {
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
        } catch (e: InvalidKeySpecException) {
            throw IllegalArgumentException("jwt.private-key 가 PKCS#8 RSA 개인키가 아닙니다.", e)
        }
        return key as? RSAPrivateCrtKey
            ?: throw IllegalArgumentException("jwt.private-key 에서 공개키를 유도할 수 없습니다(CRT 파라미터 없음).")
    }
}
