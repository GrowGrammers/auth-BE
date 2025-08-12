package com.wq.demo.integration._tnote

import com.wq.demo.shared.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "_t_note")
class _TNote(
    @Column(nullable = false) var title: String = "t",
    @Column(nullable = false) var publishedAt: Instant = Instant.now()
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}