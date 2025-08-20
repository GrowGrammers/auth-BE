package com.wq.auth.api.domain.member.entity

import com.wq.auth.shared.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "auth_provider")
open class AuthProviderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    open var member: MemberEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    open var providerType: ProviderType,

    @Column(name = "provider_id", nullable = false)
    open var providerId: String,

    open val email: String = "",
) : BaseEntity()
