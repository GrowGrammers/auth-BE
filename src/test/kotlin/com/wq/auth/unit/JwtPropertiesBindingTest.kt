package com.wq.demo.unit

import com.wq.demo.jwt.JwtProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@SpringBootTest
@ConfigurationPropertiesScan
@TestPropertySource(properties = [
    // 32바이트(256bit) Base64 시크릿 예시
    "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE===",
    "jwt.access-exp=15m",
    "jwt.refresh-exp=14d"
])
class JwtPropertiesBindingTest : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var props: JwtProperties

    init {
        test("JwtProperties 가 yml 값으로 정상 바인딩된다") {
            props.secret shouldBe "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE==="
            props.accessExp shouldBe Duration.ofMinutes(15)
            props.refreshExp shouldBe Duration.ofDays(14)
        }
    }
}