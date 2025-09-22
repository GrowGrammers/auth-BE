package com.wq.auth.shared.time

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZoneId

/** 응답에 사용할 기본 타임존 제공자 */
interface ZoneProvider { fun zoneId(): ZoneId }

/** APP_TIME_DEFAULT_ZONE 없으면 기본 Asia/Seoul */
@Component
class PropertiesZoneProvider(
    @Value("\${app.time.default-zone}")
    private val defaultZone: String
) : ZoneProvider {
    override fun zoneId(): ZoneId = ZoneId.of(defaultZone)
}