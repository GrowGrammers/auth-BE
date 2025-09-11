package com.wq.auth.unit

import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.member.MemberService
import com.wq.auth.api.domain.member.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.member.entity.ProviderType
import com.wq.auth.api.domain.member.AuthProviderRepository
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.member.RefreshTokenRepository
import com.wq.auth.api.domain.member.entity.RefreshTokenEntity
import com.wq.auth.api.domain.member.error.MemberException
import com.wq.auth.api.domain.member.error.MemberExceptionCode
import com.wq.auth.security.jwt.JwtProperties
import com.wq.auth.security.jwt.JwtProvider
import com.wq.auth.security.jwt.error.JwtException
import com.wq.auth.security.jwt.error.JwtExceptionCode
import com.wq.auth.shared.utils.NicknameGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.kotlin.*
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.util.Optional

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
            val opaqueId = "opaque-id-123"
            val accessToken = "access.token.here"
            val refreshToken = "refresh.token.here"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)
            whenever(mockAuthProvider.member).thenReturn(mockMember)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(refreshTokenRepository.findByMember(mockMember)).thenReturn(null)
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
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
            val opaqueId = "opaque-id-123"
            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()
            val existingRefreshToken = mock<RefreshTokenEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)
            whenever(mockAuthProvider.member).thenReturn(mockMember)
            whenever(mockAuthProvider.email).thenReturn(email)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(refreshTokenRepository.findByMember(mockMember)).thenReturn(existingRefreshToken)
            whenever(jwtProvider.createAccessToken(opaqueId, mockMember.role)).thenReturn("access-token")
            whenever(jwtProvider.createRefreshToken(eq(opaqueId), any())).thenReturn("refresh-token")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

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
            val opaqueId = "opaque-id-456"
            val accessToken = "new.access.token"
            val refreshToken = "new.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(null)
            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
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
            verify(authEmailService).validateEmailFormat(email)
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
            val opaqueId = "opaque-id-789"
            val accessToken = "signup.access.token"
            val refreshToken = "signup.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

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
            val opaqueId = "opaque-id-duplicate"
            val accessToken = "duplicate.access.token"
            val refreshToken = "duplicate.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)

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
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

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
            val opaqueId = "opaque-id-unique"

            val mockMember = mock<MemberEntity>()
            whenever(mockMember.id).thenReturn(1L)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockMember.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)

            whenever(nicknameGenerator.generate())
                .thenReturn(duplicateNickname, duplicateNickname, duplicateNickname, uniqueNickname)
            whenever(memberRepository.existsByNickname(duplicateNickname)).thenReturn(true)
            whenever(memberRepository.existsByNickname(uniqueNickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn("token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("refresh")
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
            val opaqueId = "opaque-id-123"
            val memberId = 1L
            val jti = "jti123"
            val mockMember = mock<MemberEntity>()
            val claims = mapOf("jti" to jti)

            whenever(mockMember.id).thenReturn(memberId)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(refreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(mockMember))

            // when
            memberService.logout(refreshToken)

            // then
            verify(refreshTokenRepository, times(1)).deleteByMemberIdAndJti(memberId, jti)
        }

        it("실패 - DB 삭제 예외 발생 시 MemberException 던짐") {
            // given
            val refreshToken = "dummyToken"
            val opaqueId = "opaque-id-123"
            val memberId = 1L
            val jti = "jti123"
            val mockMember = mock<MemberEntity>()
            val claims = mapOf("jti" to jti)

            whenever(mockMember.id).thenReturn(memberId)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(refreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(mockMember))
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

    describe("AccessToken 재발급") {
        it("유효한 refreshToken이 주어졌을 때 새로운 토큰들을 생성하고 반환해야 한다") {
            // given
            val refreshToken = "valid-refresh-token"
            val opaqueId = "opaque-id-123"
            val jti = "test-jti"
            val memberId = 1L
            val member = mock<MemberEntity>()
            val claims = mapOf("jti" to jti)

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            val accessExp = Duration.ofMinutes(30)
            val refreshExp = Duration.ofDays(7)

            whenever(member.id).thenReturn(memberId)
            whenever(member.opaqueId).thenReturn(opaqueId)
            whenever(member.role).thenReturn(com.wq.auth.api.domain.member.entity.Role.MEMBER)

            // mocking
            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(refreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            whenever(refreshTokenRepository.findByMemberIdAndJti(memberId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any())).thenReturn(newAccessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(newRefreshToken)
            whenever(jwtProperties.accessExp).thenReturn(accessExp)
            whenever(jwtProperties.refreshExp).thenReturn(refreshExp)
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock<RefreshTokenEntity>())

            // when
            val result = memberService.refreshAccessToken(refreshToken)

            // then
            result.accessToken shouldBe newAccessToken
            result.refreshToken shouldBe newRefreshToken
            result.accessTokenExpiredAt shouldNotBe null

            verify(jwtProvider, times(1)).validateOrThrow(refreshToken)
            verify(refreshTokenRepository, times(1)).findByMemberIdAndJti(memberId, jti)
            verify(jwtProvider, times(1)).createAccessToken(any(), any())
            verify(jwtProvider, times(1)).createRefreshToken(any(), any())
            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
            verify(refreshTokenRepository, times(1)).save(any<RefreshTokenEntity>())
        }

        it("유효하지 않은 refreshToken이 주어졌을 때 JWT 예외를 던져야 한다") {
            val invalidRefreshToken = "invalid-refresh-token"

            whenever(jwtProvider.validateOrThrow(invalidRefreshToken)).thenThrow(
                JwtException(JwtExceptionCode.INVALID_SIGNATURE)
            )

            shouldThrow<JwtException> {
                memberService.refreshAccessToken(invalidRefreshToken)
            }

            verify(jwtProvider, times(1)).validateOrThrow(invalidRefreshToken)
            verify(refreshTokenRepository, never()).findByMemberIdAndJti(any(), any())
        }

        it("DB에 refreshToken이 존재하지 않을 때 MemberException을 던져야 한다") {
            val refreshToken = "valid-but-not-in-db-token"
            val opaqueId = "opaque-id-123"
            val jti = "test-jti"
            val memberId = 1L
            val member = mock<MemberEntity>()
            val claims = mapOf("jti" to jti)

            whenever(member.id).thenReturn(memberId)

            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(refreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            whenever(refreshTokenRepository.findByMemberIdAndJti(memberId, jti)).thenReturn(null)

            shouldThrow<MemberException> {
                memberService.refreshAccessToken(refreshToken)
            }.code shouldBe MemberExceptionCode.REFRESHTOKEN_DATABASE_FIND_FAILED

            verify(jwtProvider, times(1)).validateOrThrow(refreshToken)
            verify(refreshTokenRepository, times(1)).findByMemberIdAndJti(memberId, jti)
        }

        it("refreshToken이 만료되었을 때 만료된 토큰을 삭제하고 JWT 만료 예외를 던져야 한다") {
            val expiredRefreshToken = "expired-refresh-token"
            val opaqueId = "opaque-id-123"
            val jti = "test-jti"
            val memberId = 1L
            val member = mock<MemberEntity>()
            val claims = mapOf("jti" to jti)

            val pastTime = Instant.now().minusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(pastTime)
            whenever(member.id).thenReturn(memberId)

            doNothing().`when`(jwtProvider).validateOrThrow(expiredRefreshToken)
            whenever(jwtProvider.getOpaqueId(expiredRefreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(expiredRefreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            whenever(refreshTokenRepository.findByMemberIdAndJti(memberId, jti)).thenReturn(refreshTokenEntity)
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)

            shouldThrow<JwtException> {
                memberService.refreshAccessToken(expiredRefreshToken)
            }.code shouldBe JwtExceptionCode.EXPIRED

            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
        }

        it("멤버가 존재하지 않을 때 MemberException을 던져야 한다") {
            val refreshToken = "valid-refresh-token"
            val opaqueId = "invalid-opaque-id"
            val jti = "test-jti"
            val claims = mapOf("jti" to jti)

            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getAllClaims(refreshToken)).thenReturn(claims)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.empty())

            shouldThrow<MemberException> {
                memberService.refreshAccessToken(refreshToken)
            }.code shouldBe MemberExceptionCode.REFRESHTOKEN_DATABASE_FIND_FAILED

            verify(memberRepository, times(1)).findByOpaqueId(opaqueId)
        }
    }
})