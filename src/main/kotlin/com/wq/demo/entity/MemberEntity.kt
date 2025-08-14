package com.wq.demo.entity

import com.wq.demo.shared.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "member")
open class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @Column(name = "provider_id")
    val providerId: String? = null,

    var nickname: String = "",

    val role: Role = Role.MEMBER,

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    ) : BaseEntity()