package com.wq.auth.security.jwt

import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.LocatorAdapter
import java.security.Key

/**
 * 파서가 서명 검증 키를 고를 때 호출된다.
 *
 * **`alg` 를 먼저 본다.** `kid` 만 보고 RSA 공개키를 돌려주면, 공개키 바이트를 HMAC 시크릿으로
 * 삼아 HS256 으로 서명한 위조 토큰이 통과할 수 있다(알고리즘 혼동). jjwt 도 키 타입과
 * `alg` 불일치를 막지만, 이 클래스 단독으로도 올바르게 둔다.
 *
 * | 헤더 | 키 |
 * |---|---|
 * | `RS256` + 현재 `kid` | RSA 공개키 |
 * | `HS256` + 레거시 시크릿 있음 | 레거시 HMAC 키 (검증 전용, 전환 기간) |
 * | 그 외 | 거부 → INVALID_SIGNATURE |
 */
class JwtKeyLocator(private val keys: JwtKeys) : LocatorAdapter<Key>() {

    override fun locate(header: JwsHeader): Key {
        return when (header.algorithm) {
            "RS256" ->
                if (header.keyId == keys.keyId) keys.publicKey
                else throw JwtException(JwtExceptionCode.INVALID_SIGNATURE)
            "HS256" ->
                keys.legacySecretKey ?: throw JwtException(JwtExceptionCode.INVALID_SIGNATURE)
            else -> throw JwtException(JwtExceptionCode.INVALID_SIGNATURE)
        }
    }
}
