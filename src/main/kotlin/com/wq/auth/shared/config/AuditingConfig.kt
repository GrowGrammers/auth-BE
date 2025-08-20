package com.wq.auth.shared.config

import com.wq.auth.shared.time.TimeProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
class AuditingConfig(private val time: TimeProvider) {
    @Bean
    fun dateTimeProvider(): DateTimeProvider =
        DateTimeProvider { Optional.of(time.nowInstant()) } // UTC Instant 기록
}