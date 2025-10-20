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

    @Column(name = "primary_email", nullable = true)
    val primaryEmail: String? = null,

    @Column(name = "phone_number", length = 20, nullable = true)
    val phoneNumber: String? = null,

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

    //TODO
    //회원 삭제 기능 개발시 모든 쿼리 is_deleted 확인 추가
    //is_deleted -> deleted_at?
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

) : BaseEntity() {

    companion object {
        fun createEmailVerifiedMember(nickname: String, email: String) =
            MemberEntity(
                nickname = nickname,
                isEmailVerified = true,
                primaryEmail = email,
                opaqueId = UUID.randomUUID().toString(),
                lastLoginAt = LocalDateTime.now(),
            )

        fun create(
            nickname: String,
            role: Role = Role.MEMBER
        ): MemberEntity {
            require(nickname.isNotBlank()) { "닉네임은 필수입니다" }
            require(nickname.length <= 100) { "닉네임은 100자를 초과할 수 없습니다" }

            return MemberEntity(
                opaqueId = UUID.randomUUID().toString(),
                nickname = nickname.trim(),
                role = role
            )
        }

        fun createSocialMember(
            nickname: String,
            isEmailVerified: Boolean = true,
            primaryEmail: String,
            role: Role = Role.MEMBER
        ): MemberEntity {
            require(nickname.isNotBlank()) { "닉네임은 필수입니다" }
            require(nickname.length <= 100) { "닉네임은 100자를 초과할 수 없습니다" }

            return MemberEntity(
                opaqueId = UUID.randomUUID().toString(),
                nickname = nickname.trim(),
                role = role,
                isEmailVerified = isEmailVerified,
                primaryEmail = primaryEmail
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
     * 최근 로그인 시간 업데이트
     */
    fun updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now()
    }

    /**
     * 회원을 soft delete 처리합니다.
     * 실제로 데이터를 삭제하지 않고 isDeleted 플래그만 변경합니다.
     */
    fun softDelete() {
        this.isDeleted = true
    }

    /**
     * 관리자 권한 확인
     */
    fun isAdmin(): Boolean = role == Role.ADMIN

    override fun toString(): String {
        return "MemberEntity(id=$id, opaqueId='$opaqueId', nickname='$nickname', role=$role)"
    }
}