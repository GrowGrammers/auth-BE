package com.wq.demo.jwt

import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class SecretKeyHolder(@Value("\${jwt.secret}") private val secret: String) {

    val key: SecretKey
        get() = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
}