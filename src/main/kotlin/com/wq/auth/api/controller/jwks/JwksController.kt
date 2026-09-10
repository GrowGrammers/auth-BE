package com.wq.auth.api.controller.jwks

import com.wq.auth.security.annotation.PublicApi
import com.wq.auth.security.jwt.JwtKeys
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/**
 * 공개키 배포 엔드포인트 (RFC 7517 JWK Set).
 *
 * 게이트웨이 없이 auth-api 의 토큰을 스스로 검증하는 서비스(lnb-api)가 기동 시 한 번 받아 캐시한다.
 * 개인 파라미터는 절대 담기지 않는다 — [JwtKeys.publicJwk] 가 공개 JWK 만 만든다.
 *
 * 표준 JWKS 클라이언트가 그대로 읽어야 하므로 CommonResponse 로 감싸지 않는다.
 */
@Tag(name = "JWKS", description = "JWT 서명 공개키 배포")
@RestController
class JwksController(
    private val jwtKeys: JwtKeys,
) {

    @Operation(
        summary = "JWKS 조회",
        description = "AT·RT 서명(RS256) 검증용 공개키 목록. 토큰 헤더의 kid 와 맞는 키로 검증한다."
    )
    @PublicApi
    // RFC 7517 은 JWK Set 의 미디어 타입으로 application/jwk-set+json 을 둔다.
    // 표준 클라이언트가 그 타입만 Accept 로 보내도 200 이 나가야 한다.
    @GetMapping(
        "/.well-known/jwks.json",
        produces = [MediaType.APPLICATION_JSON_VALUE, "application/jwk-set+json"]
    )
    fun jwks(): ResponseEntity<Map<String, Any>> {
        // RsaPublicJwk 는 Map 이지만 jjwt 고유 타입이라 직렬화 특성을 타지 않도록 평범한 Map 으로 복사한다.
        val key: Map<String, Any> = LinkedHashMap(jwtKeys.publicJwk)
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(mapOf("keys" to listOf(key)))
    }
}
