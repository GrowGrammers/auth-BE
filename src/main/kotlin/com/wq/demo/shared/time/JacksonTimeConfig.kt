package com.wq.demo.shared.time

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ISO_WITH_SECONDS: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

/** Instant -> ISO-8601(+offset)로 ZoneProvider 기준 변환 */
class InstantToZoneSerializer(
    private val zoneProvider: ZoneProvider
) : JsonSerializer<Instant>() { // Instant -> JSON 문자열로 변환하는 클래스
    override fun serialize(value: Instant?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) { gen.writeNull(); return }
        val offset = value
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(zoneProvider.zoneId())
            .toOffsetDateTime()
        gen.writeString(ISO_WITH_SECONDS.format(offset)) // ← 항상 HH:mm:ss 출력
    }
}

/** ISO 문자열 -> Instant (요청 바디 수신 시) */
class InstantFromIsoDeserializer : JsonDeserializer<Instant>() {
    override fun deserialize(p: com.fasterxml.jackson.core.JsonParser, ctxt: DeserializationContext): Instant {
        val s = p.text
        return runCatching { OffsetDateTime.parse(s).toInstant() }
            .getOrElse { Instant.parse(s) }
    }
}

/**
 * Jackson에 커스텀 Serializer/Deserializer를 등록하는 설정 클래스
 */
@Configuration
class JacksonTimeConfig(private val zoneProvider: ZoneProvider) {
    @Bean
    fun instantZoneModule(): SimpleModule =
        SimpleModule().apply {
            addSerializer(Instant::class.java, InstantToZoneSerializer(zoneProvider))
            addDeserializer(Instant::class.java, InstantFromIsoDeserializer())
        }
}