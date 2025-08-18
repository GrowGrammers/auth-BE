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
    open val providerId: String? = null,

    open var nickname: String = "",

    @Enumerated(EnumType.STRING)
    open val role: Role = Role.MEMBER,

    @Column(name = "is_email_verified", nullable = false)
    open var isEmailVerified: Boolean = false,

    @Column(name = "last_login_at")
    open var lastLoginAt: LocalDateTime? = null,

    @Column(name = "is_deleted", nullable = false)
    open var isDeleted: Boolean = false,

    ) : BaseEntity()