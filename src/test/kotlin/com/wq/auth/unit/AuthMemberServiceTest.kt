package com.wq.auth.unit

import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.api.domain.member.AuthProviderRepository
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.shared.utils.NicknameGenerator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
class AuthMemberServiceTest : DescribeSpec({

    lateinit var memberService: MemberService
    lateinit var authProviderRepository: AuthProviderRepository
    lateinit var memberRepository: MemberRepository
    lateinit var jwtProvider: JwtProvider
    lateinit var jwtProperties: JwtProperties
    lateinit var nicknameGenerator: NicknameGenerator

    beforeEach {
        authProviderRepository = mock()
        memberRepository = mock()
        jwtProvider = mock()
        jwtProperties = mock()
        nicknameGenerator = mock()

        memberService = MemberService(
            authProviderRepository = authProviderRepository,
            memberRepository = memberRepository,
            jwtProvider = jwtProvider,
            jwtProperties = jwtProperties,
            nicknameGenerator = nicknameGenerator
        )
    }

    describe("이메일 로그인 테스트") {

        it("기존 사용자가 이메일로 로그인하면 JWT 토큰을 반환한다") {
            // given
            val email = "test@example.com"
            val memberId = 1L
            val nickname = "testUser"
            val accessToken = "access.token.here"
            val refreshToken = "refresh.token.here"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockAuthProvider.member).thenReturn(mockMember)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))

            // when
            val result = memberService.emailLogin(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.expiredAt shouldNotBe null

            verify(authProviderRepository).findByEmail(email)
            verify(jwtProvider).createAccessToken(any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
        }

        it("존재하지 않는 이메일로 로그인하면 회원가입을 진행한다") {
            // given
            val email = "newuser@example.com"
            val memberId = 2L
            val nickname = "newUser123"
            val accessToken = "new.access.token"
            val refreshToken = "new.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(null)
            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))

            // when
            val result = memberService.emailLogin(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.expiredAt shouldNotBe null

            verify(authProviderRepository).findByEmail(email)
            verify(nicknameGenerator).generate()
            verify(memberRepository).existsByNickname(nickname)
            verify(memberRepository).save(any<MemberEntity>())
            verify(authProviderRepository).save(any<AuthProviderEntity>())
        }
    }

    describe("회원가입 테스트") {

        it("새로운 이메일로 회원가입하면 계정을 생성하고 JWT 토큰을 반환한다") {
            // given
            val email = "signup@example.com"
            val memberId = 3L
            val nickname = "signupUser456"
            val accessToken = "signup.access.token"
            val refreshToken = "signup.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))

            // when
            val result = memberService.signUp(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.expiredAt shouldNotBe null

            verify(nicknameGenerator).generate()
            verify(memberRepository).existsByNickname(nickname)
            verify(memberRepository).save(any<MemberEntity>())
            verify(authProviderRepository).save(any<AuthProviderEntity>())
            verify(jwtProvider).createAccessToken(any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
        }

        it("중복된 닉네임이 생성되면 새로운 닉네임을 재생성한다") {
            // given
            val email = "duplicate@example.com"
            val memberId = 4L
            val duplicateNickname = "duplicate123"
            val uniqueNickname = "unique456"
            val accessToken = "duplicate.access.token"
            val refreshToken = "duplicate.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)

            whenever(nicknameGenerator.generate())
                .thenReturn(duplicateNickname)
                .thenReturn(uniqueNickname)
            whenever(memberRepository.existsByNickname(duplicateNickname)).thenReturn(true)
            whenever(memberRepository.existsByNickname(uniqueNickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))

            // when
            val result = memberService.signUp(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken

            verify(nicknameGenerator, times(2)).generate()
            verify(memberRepository).existsByNickname(duplicateNickname)
            verify(memberRepository).existsByNickname(uniqueNickname)
        }

        it("회원가입시 Member 엔티티의 속성이 올바르게 설정된다") {
            // given
            val email = "entity@example.com"
            val nickname = "entityUser"

            val memberCaptor = argumentCaptor<MemberEntity>()
            val providerCaptor = argumentCaptor<AuthProviderEntity>()

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(memberCaptor.capture())).thenAnswer { it.arguments[0] as MemberEntity }
            whenever(authProviderRepository.save(providerCaptor.capture())).thenAnswer { it.arguments[0] as AuthProviderEntity }
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn("token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("refresh")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(1800000L))

            // when
            memberService.signUp(email)

            // then
            val savedMember = memberCaptor.firstValue
            val savedProvider = providerCaptor.firstValue

            savedMember.nickname shouldBe nickname
            savedMember.isEmailVerified shouldBe true
            savedProvider.providerType shouldBe ProviderType.EMAIL
            savedProvider.email shouldBe email
            savedProvider.member shouldBe savedMember
        }
    }
})