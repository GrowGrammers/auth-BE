package com.wq.auth.api.domain.member.entity

import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "member",
    indexes = [
        Index(name = "idx_member_opaque_id", columnList = "opaque_id", unique = true)
    ]
)
open class MemberEntity protected constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "provider_id")
    val providerId: String? = null,

    @Column(name = "opaque_id", nullable = false, unique = true, length = 36)
    val opaqueId: String,

    @Column(nullable = false, length = 100)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.MEMBER,

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

) : BaseEntity() {

    companion object {
        fun createEmailVerifiedMember(nickname: String) =
            MemberEntity(
                nickname = nickname,
                isEmailVerified = true,
                opaqueId = UUID.randomUUID().toString(),
            )

        fun create(
            nickname: String,
            role: Role = Role.MEMBER
        ): MemberEntity {
            require(nickname.isNotBlank()) { "닉네임은 필수입니다" }
            require(nickname.length <= 100) { "닉네임은 100자를 초과할 수 없습니다" }

            return MemberEntity(
                providerId = null,
                opaqueId = UUID.randomUUID().toString(),
                nickname = nickname.trim(),
                role = role
            )
        }

        fun createSocialMember(
            providerId: String,
            nickname: String,
            isEmailVerified: Boolean = true,
            role: Role = Role.MEMBER
        ): MemberEntity {
            require(providerId.isNotBlank()) { "소셜 제공자 ID는 필수입니다" }
            require(nickname.isNotBlank()) { "닉네임은 필수입니다" }
            require(nickname.length <= 100) { "닉네임은 100자를 초과할 수 없습니다" }

            return MemberEntity(
                providerId = providerId,
                opaqueId = UUID.randomUUID().toString(),
                nickname = nickname.trim(),
                role = role,
                isEmailVerified = isEmailVerified
            )
        }
    }

    /**
     * 이메일 인증 완료 처리
     */
    fun verifyEmail() {
        this.isEmailVerified = true
    }

    /**
     * 관리자 권한 확인
     */
    fun isAdmin(): Boolean = role == Role.ADMIN

    override fun toString(): String {
        return "MemberEntity(id=$id, opaqueId='$opaqueId', nickname='$nickname', role=$role)"
    }
}