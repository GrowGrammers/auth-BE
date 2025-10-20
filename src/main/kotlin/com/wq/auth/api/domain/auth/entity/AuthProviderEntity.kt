package com.wq.auth.api.domain.auth.entity

import com.wq.auth.api.domain.member.entity.MemberEntity
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

    @Column(name = "provider_id", nullable = true)
    open var providerId: String? = null,

    @Column(name = "email")
    open var email: String = "",
) : BaseEntity() {

    /**
     * 소셜 제공자 정보를 업데이트합니다.
     * 
     * @param newProviderId 새로운 제공자 ID
     * @param newEmail 새로운 이메일
     */
    fun updateProviderInfo(newProviderId: String, newEmail: String) {
        this.providerId = newProviderId
        this.email = newEmail
    }

    /**
     * AuthProvider의 소유 회원을 변경합니다.
     * 계정 병합시 사용됩니다.
     */
    fun changeMember(newMember: MemberEntity) {
        this.member = newMember
    }

    companion object {
        fun createEmailProvider(member: MemberEntity, email: String) =
            AuthProviderEntity(
                member = member,
                providerType = ProviderType.EMAIL,
                email = email
            )
    }
}
