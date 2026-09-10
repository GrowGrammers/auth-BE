package com.wq.auth.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /** RS256 서명용 PKCS#8 개인키. PEM 헤더·푸터·개행을 뺀 한 줄 base64. 필수. */
    val privateKey: String,
    /**
     * HS256 레거시 **검증 전용** 시크릿(base64). 비어 있으면 HS256 토큰을 거부한다.
     * HS256 으로 발급되던 시절의 토큰이 RT 만료 기간 동안 살아 있으므로 전환 기간에만 둔다.
     */
    val legacySecret: String = "",
    val accessExp: Duration,
    val refreshExp: Duration,
)
