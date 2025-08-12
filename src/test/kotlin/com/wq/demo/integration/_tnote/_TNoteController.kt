package com.wq.demo.integration._tnote

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/test-notes")
class _TNoteController(
    private val repo: _TNoteRepository
) {
    data class Dto(
        val id: Long,
        val title: String,
        val publishedAt: Instant,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): Dto =
        repo.findById(id).orElseThrow().let {
            Dto(
                id = it.id!!,
                title = it.title,
                publishedAt = it.publishedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
}