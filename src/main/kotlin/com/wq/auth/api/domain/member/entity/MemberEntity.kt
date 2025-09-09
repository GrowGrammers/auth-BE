package com.wq.auth.api.domain.member.entity

import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

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

    @Column(name = "opaque_id", nullable = false, unique = true, updatable = false, length = 64)
    open val opaqueId: String = UUID.randomUUID().toString(),

    @Column(name = "phone_number", length = 20, unique = true)
    open var phoneNumber: String? = null

) : BaseEntity() {
    companion object {
        fun createEmailVerifiedMember(nickname: String) =
            MemberEntity(
                nickname = nickname,
                isEmailVerified = true
            )
    }
}