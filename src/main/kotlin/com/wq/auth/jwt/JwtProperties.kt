package com.wq.demo.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties (
    val secret: String,
    val accessExp: Duration,
    val refreshExp: Duration
)