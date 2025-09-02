package com.wq.auth.unit

import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import com.wq.auth.jwt.JwtProperties
import com.wq.auth.jwt.JwtProvider
import com.wq.auth.api.domain.member.AuthProviderRepository
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.member.RefreshTokenRepository
import com.wq.auth.api.domain.member.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.api.domain.member.error.MemberExceptionCode
import com.wq.auth.shared.utils.NicknameGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
class AuthMemberServiceTest : DescribeSpec({

    lateinit var memberService: MemberService
    lateinit var authEmailService: AuthEmailService
    lateinit var authProviderRepository: AuthProviderRepository
    lateinit var memberRepository: MemberRepository
    lateinit var refreshTokenRepository: RefreshTokenRepository
    lateinit var jwtProvider: JwtProvider
    lateinit var jwtProperties: JwtProperties
    lateinit var nicknameGenerator: NicknameGenerator

    beforeEach {
        authProviderRepository = mock()
        memberRepository = mock()
        refreshTokenRepository = mock()
        authEmailService = mock()
        jwtProvider = mock()
        jwtProperties = mock()
        nicknameGenerator = mock()

        memberService = MemberService(
            authProviderRepository = authProviderRepository,
            memberRepository = memberRepository,
            refreshTokenRepository = refreshTokenRepository,
            authEmailService = authEmailService,
            jwtProvider = jwtProvider,
            jwtProperties = jwtProperties,
            nicknameGenerator = nicknameGenerator,
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
            val jti = "jwt-id-123"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockAuthProvider.member).thenReturn(mockMember)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair(refreshToken, jti))
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            val result = memberService.emailLogin(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.expiredAt shouldNotBe null

            verify(authProviderRepository).findByEmail(email)
            verify(jwtProvider).createAccessToken(any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
            verify(refreshTokenRepository).save(any<RefreshTokenEntity>())
            verifyNoInteractions(authEmailService)
        }

        it("기존 사용자 로그인 시 이전 refresh token이 있으면 삭제 후 새로 생성한다") {
            // given
            val email = "test@example.com"
            val memberId = 1L
            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()
            val existingRefreshToken = mock<RefreshTokenEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockAuthProvider.member).thenReturn(mockMember)
            whenever(mockAuthProvider.email).thenReturn(email)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(refreshTokenRepository.findByMember(mockMember)).thenReturn(existingRefreshToken)
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn("access-token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair("refresh-token", "jti"))
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))

            // when
            memberService.emailLogin(email)

            // then
            verify(refreshTokenRepository).delete(existingRefreshToken)
            verify(refreshTokenRepository).save(any<RefreshTokenEntity>())
        }

        it("존재하지 않는 이메일로 로그인하면 회원가입을 진행한다") {
            // given
            val email = "newuser@example.com"
            val memberId = 2L
            val nickname = "newUser123"
            val accessToken = "new.access.token"
            val refreshToken = "new.refresh.token"
            val jti = "new-jwt-id"
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
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair(refreshToken, jti))
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
            val jti = "signup-jwt-id"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair(refreshToken, jti))
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))

            // when
            val result = memberService.signUp(email)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.expiredAt shouldNotBe null

            verify(authEmailService).validateEmailFormat(email)
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
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair(refreshToken, "jti"))
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

        it("닉네임 중복이 계속 발생하는 경우를 테스트한다") {
            // given
            val email = "test@example.com"
            val duplicateNickname = "duplicate"
            val uniqueNickname = "unique"

            val mockMember = mock<MemberEntity>()
            whenever(mockMember.id).thenReturn(1L)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)

            whenever(nicknameGenerator.generate())
                .thenReturn(duplicateNickname, duplicateNickname, duplicateNickname, uniqueNickname)
            whenever(memberRepository.existsByNickname(duplicateNickname)).thenReturn(true)
            whenever(memberRepository.existsByNickname(uniqueNickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn("token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair("refresh", "jti"))
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            memberService.signUp(email)

            // then
            verify(nicknameGenerator, times(4)).generate()
        }

        it("잘못된 이메일 형식으로 회원가입 시 예외가 발생한다") {
            // given
            val invalidEmail = "invalid-email"
            val exception = RuntimeException("Invalid email format")

            whenever(authEmailService.validateEmailFormat(invalidEmail)).thenThrow(exception)

            // when & then
            shouldThrow<RuntimeException> {
                memberService.signUp(invalidEmail)
            }

            verify(authEmailService).validateEmailFormat(invalidEmail)
            verifyNoInteractions(nicknameGenerator)
            verifyNoInteractions(memberRepository)
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
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(Pair("refresh", "jti"))
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

        it("데이터베이스 저장 실패 시 MemberException이 발생한다") {
            // given
            val email = "test@example.com"
            val nickname = "testUser"
            val dbException = RuntimeException("Database connection failed")

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenThrow(dbException)

            // when & then
            val exception = shouldThrow<MemberException> {
                memberService.signUp(email)
            }

            exception.code shouldBe MemberExceptionCode.DATABASE_SAVE_FAILED
            exception.cause shouldBe dbException
        }
    }

    describe("로그아웃 테스트") {

        it("성공 - refreshToken 삭제 호출") {
            // given
            val refreshToken = "dummyToken"
            val memberId = 1L
            val jti = "jti123"

            whenever(jwtProvider.getSubject(refreshToken)).thenReturn(memberId.toString())
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)

            // when
            memberService.logout(refreshToken)

            // then
            verify(refreshTokenRepository, times(1)).deleteByMemberIdAndJti(memberId, jti)
        }

        it("실패 - DB 삭제 예외 발생 시 MemberException 던짐") {
            // given
            val refreshToken = "dummyToken"
            val memberId = 1L
            val jti = "jti123"

            whenever(jwtProvider.getSubject(refreshToken)).thenReturn(memberId.toString())
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(refreshTokenRepository.deleteByMemberIdAndJti(memberId, jti))
                .thenThrow(RuntimeException("DB error"))

            // when
            val ex = shouldThrow<MemberException> {
                memberService.logout(refreshToken)
            }

            // then
            ex.code shouldBe MemberExceptionCode.LOGOUT_FAILED
            verify(refreshTokenRepository, times(1)).deleteByMemberIdAndJti(memberId, jti)
        }
    }
})