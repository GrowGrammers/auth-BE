package com.wq.auth.unit

import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

class MemberEntityTest : StringSpec({

    "MemberEntity.create()를 통해 정상적으로 생성된다" {
        // Given & When
        val member = MemberEntity.create(
            nickname = "테스트사용자",
            role = Role.MEMBER
        )

        // Then
        member.nickname shouldBe "테스트사용자"
        member.role shouldBe Role.MEMBER
        member.opaqueId shouldNotBe null
        UUID.fromString(member.opaqueId) // UUID 형식 검증
        member.isEmailVerified shouldBe false
        member.isDeleted shouldBe false
    }

    "providerId를 포함하여 생성할 수 있다" {
        // Given & When
        val member = MemberEntity.create(
            providerId = "google_123456",
            nickname = "구글사용자",
            role = Role.ADMIN
        )

        // Then
        member.providerId shouldBe "google_123456"
        member.nickname shouldBe "구글사용자"
        member.role shouldBe Role.ADMIN
    }

    "닉네임이 공백이면 예외가 발생한다" {
        // Given & When & Then
        shouldThrow<IllegalArgumentException> {
            MemberEntity.create(nickname = "")
        }.message shouldBe "닉네임은 필수입니다"
    }

    "닉네임이 100자를 초과하면 예외가 발생한다" {
        // Given
        val longNickname = "a".repeat(101)

        // When & Then
        shouldThrow<IllegalArgumentException> {
            MemberEntity.create(nickname = longNickname)
        }.message shouldBe "닉네임은 100자를 초과할 수 없습니다"
    }

    "닉네임 앞뒤 공백이 자동으로 제거된다" {
        // Given & When
        val member = MemberEntity.create(nickname = "  테스트  ")

        // Then
        member.nickname shouldBe "테스트"
    }

    "이메일 인증 처리가 정상 작동한다" {
        // Given
        val member = MemberEntity.create(nickname = "테스트")

        // When
        member.verifyEmail()

        // Then
        member.isEmailVerified shouldBe true
    }

    "관리자 권한 확인이 정상 작동한다" {
        // Given
        val adminMember = MemberEntity.create(nickname = "관리자", role = Role.ADMIN)
        val regularMember = MemberEntity.create(nickname = "일반사용자", role = Role.MEMBER)

        // When & Then
        adminMember.isAdmin() shouldBe true
        regularMember.isAdmin() shouldBe false
    }

    /* 
    // 다음 코드는 컴파일 에러가 발생해야 함 (protected constructor)
    "외부에서 직접 생성자 호출 시도" {
        // 이 코드는 컴파일되지 않아야 함
        // val member = MemberEntity(
        //     opaqueId = "test-uuid",
        //     nickname = "테스트"
        // )
    }
    */
})

