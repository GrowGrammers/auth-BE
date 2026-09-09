package com.wq.auth.security.jwt

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JwtKeysConfig {

    /** 키가 없거나 깨져 있으면 여기서 기동이 실패한다. 의도한 동작이다. */
    @Bean
    fun jwtKeys(jwtProperties: JwtProperties): JwtKeys =
        JwtKeys(jwtProperties.privateKey, jwtProperties.legacySecret)
}
