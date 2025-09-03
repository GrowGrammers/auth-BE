package com.wq.auth.api.domain.member.entity

import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class refreshTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: MemberEntity,

    @Column(nullable = false, unique = true)
    var jti: String,

    @Column(name = "expired_at", nullable = false)
    var expiredAt: Instant,

    )  : BaseEntity() {
    companion object {
        fun of(member: MemberEntity, jti: String, expiredAt: Instant): refreshTokenEntity {
            return refreshTokenEntity(
                member = member,
                jti = jti,
                expiredAt = expiredAt
            )
        }
    }
}