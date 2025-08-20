package com.wq.auth.unit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.wq.auth.shared.time.ZoneProvider
import io.kotest.core.spec.style.StringSpec
import java.time.Instant
import java.time.ZoneId
import com.fasterxml.jackson.databind.module.SimpleModule
import com.wq.auth.shared.time.InstantFromIsoDeserializer
import com.wq.auth.shared.time.InstantToZoneSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class JacksonInstantModuleTest : StringSpec({

    fun mapperWith(zoneId: String): ObjectMapper {
        val zoneProvider = object : ZoneProvider {
            override fun zoneId(): ZoneId = ZoneId.of(zoneId)
        }
        val module = SimpleModule().apply {
            addSerializer(Instant::class.java, InstantToZoneSerializer(zoneProvider))
            addDeserializer(Instant::class.java, InstantFromIsoDeserializer())
        }
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(module)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    "Instant → JSON 직렬화 시 Asia/Seoul(+09:00)로 변환된다" {
        val mapper = mapperWith("Asia/Seoul")
        val instant = Instant.parse("2025-08-12T00:00:00Z")

        val json = mapper.writeValueAsString(mapOf("at" to instant))
        json shouldContain "\"at\":\"2025-08-12T09:00:00+09:00\""
    }

    "Instant → JSON 직렬화 시 Europe/Paris(+02:00 또는 +01:00)로 변환된다" {
        val mapper = mapperWith("Europe/Paris")
        val instant = Instant.parse("2025-08-12T00:00:00Z")

        val json = mapper.writeValueAsString(mapOf("at" to instant))
        json shouldContain "+02:00" // 8월은 CEST(+02:00)
    }

    "오프셋 포함 ISO 문자열 → Instant(UTC)로 역직렬화된다" {
        val mapper = mapperWith("Asia/Seoul")
        val json = """{"at":"2025-08-12T09:00:00+09:00"}"""

        val node = mapper.readTree(json)
        val inst = mapper.treeToValue(node.get("at"), Instant::class.java)

        inst shouldBe Instant.parse("2025-08-12T00:00:00Z")
    }

    "UTC(Z) 문자열 → Instant(UTC)로 역직렬화된다" {
        val mapper = mapperWith("Asia/Seoul")
        val json = """{"at":"2025-08-12T00:00:00Z"}"""

        val node = mapper.readTree(json)
        val inst = mapper.treeToValue(node.get("at"), Instant::class.java)

        inst shouldBe Instant.parse("2025-08-12T00:00:00Z")
    }
})