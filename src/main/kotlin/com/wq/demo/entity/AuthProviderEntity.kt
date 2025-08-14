package com.wq.demo.entity

import com.wq.demo.shared.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "auth_provider")
open class AuthProviderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: MemberEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    var providerType: ProviderType,

    @Column(name = "provider_id", nullable = false)
    var providerId: String,

    val email: String = "",
) : BaseEntity()
