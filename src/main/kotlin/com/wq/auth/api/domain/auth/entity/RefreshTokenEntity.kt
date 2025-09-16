package com.wq.auth.api.domain.auth.entity

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: MemberEntity,

    @Column(nullable = false, unique = true)
    var jti: String,

    @Column(name = "expired_at", nullable = true)
    var expiredAt: Instant? = null,

    @Column(name = "opaque_id", nullable = false, length = 64)
    val opaqueId: String,

    @Column(name = "device_id", nullable = true, length = 64)
    val deviceId: String? = null // 웹이면 null, 앱이면 UUID

)  : BaseEntity() {
    companion object {
        fun ofWeb(member: MemberEntity, jti: String, expiredAt: Instant, opaqueId: String): RefreshTokenEntity {
            return RefreshTokenEntity(
                member = member,
                jti = jti,
                expiredAt = expiredAt,
                opaqueId = opaqueId,
                deviceId = null
            )
        }

        fun ofApp(member: MemberEntity, jti: String, expiredAt: Instant, opaqueId: String, deviceId: String): RefreshTokenEntity {
            return RefreshTokenEntity(
                member = member,
                jti = jti,
                expiredAt = expiredAt,
                opaqueId = opaqueId,
                deviceId = deviceId
            )
        }

        fun of(member: MemberEntity, jti: String, opaqueId: String): RefreshTokenEntity {
            return RefreshTokenEntity(
                member = member,
                jti = jti,
                opaqueId = opaqueId
            )
        }
    }
}