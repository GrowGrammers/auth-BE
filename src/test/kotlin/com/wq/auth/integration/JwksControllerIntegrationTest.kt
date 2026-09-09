package com.wq.auth.integration

import com.wq.auth.security.jwt.JwtKeys
import com.wq.auth.support.TestJwtKeys
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.emptyString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * JWKS 는 lnb-api 같은 외부 검증자가 표준 클라이언트로 읽는다.
 * 그래서 CommonResponse 로 감싸지 않은 RFC 7517 형식이어야 한다.
 */
@SpringBootTest(
    properties = [
        "INTERNAL_API_SECRET=test-internal-secret",
        "INTERNAL_LOGGING_SECRET=test-logging-secret",
        "SECURITY_ALERT_CHAT_WEBHOOK_URL=",
        "spring.datasource.url=jdbc:h2:mem:jwks-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.private-key=" + TestJwtKeys.PRIVATE_KEY_B64,
        "jwt.access-exp=15m",
        "jwt.refresh-exp=14d",
        "spring.mail.host=localhost",
        "spring.mail.port=25",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
    ]
)
@AutoConfigureMockMvc
class JwksControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtKeys: JwtKeys

    @Test
    fun `토큰 없이 JWKS 를 읽을 수 있고 공개 파라미터만 담긴다`() {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk)
            // Spring 이 지시어를 max-age 먼저 내보낸다. 의미는 public + 1시간 캐시로 동일하다.
            .andExpect(header().string("Cache-Control", "max-age=3600, public"))
            .andExpect(jsonPath("$.keys.length()").value(1))
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].kid").value(jwtKeys.keyId))
            .andExpect(jsonPath("$.keys[0].e").value("AQAB"))
            .andExpect(jsonPath("$.keys[0].n").value(not(emptyString())))
            .andExpect(jsonPath("$.keys[0].d").doesNotExist())
            .andExpect(jsonPath("$.keys[0].p").doesNotExist())
            .andExpect(jsonPath("$.success").doesNotExist())   // CommonResponse 로 감싸지 않는다
    }
}
