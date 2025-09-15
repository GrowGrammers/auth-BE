package com.wq.auth.unit

import com.wq.auth.api.domain.email.AuthEmailService
import com.wq.auth.api.domain.auth.entity.AuthProviderEntity
import com.wq.auth.api.domain.member.entity.MemberEntity
import com.wq.auth.api.domain.auth.entity.ProviderType
import com.wq.auth.api.domain.auth.AuthProviderRepository
import com.wq.auth.api.domain.auth.AuthService
import com.wq.auth.api.domain.member.MemberRepository
import com.wq.auth.api.domain.auth.RefreshTokenRepository
import com.wq.auth.api.domain.auth.entity.RefreshTokenEntity
import com.wq.auth.api.domain.auth.error.AuthException
import com.wq.auth.api.domain.auth.error.AuthExceptionCode
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
class AuthServiceTest : DescribeSpec({

    lateinit var authService: AuthService
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

        authService = AuthService(
            authEmailService = authEmailService,
            memberRepository = memberRepository,
            authProviderRepository = authProviderRepository,
            refreshTokenRepository = refreshTokenRepository,
            jwtProvider = jwtProvider,
            jwtProperties = jwtProperties,
            nicknameGenerator = nicknameGenerator,
        )
    }

    describe("이메일 로그인 테스트") {

        it("기존 사용자가 이메일로 로그인하면 JWT 토큰을 반환한다 - Web") {
            // given
            val email = "test@example.com"
            val deviceId: String? = null
            val clientType = "web"
            val memberId = 1L
            val nickname = "testUser"
            val opaqueId = "test-opaque-id"
            val accessToken = "access.token.here"
            val refreshToken = "refresh.token.here"
            val jti = "jwt-id-123"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()
            val mockAuthProvider = mock<AuthProviderEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)
            whenever(mockAuthProvider.email).thenReturn(email)
            whenever(mockAuthProvider.member).thenReturn(mockMember)

            whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.findByMemberAndDeviceId(mockMember, deviceId)).thenReturn(null)
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            val result = authService.emailLogin(email, deviceId, clientType)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.accessTokenExpiredAt shouldNotBe null
            result.refreshTokenExpiredAt shouldNotBe null

            verify(authProviderRepository).findByEmail(email)
            verify(jwtProvider).createAccessToken(any(), any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
            verify(refreshTokenRepository, times(1)).save(any<RefreshTokenEntity>())
        }

        it("유효한 refreshToken이 주어졌을 때 새로운 토큰들을 생성하고 반환해야 한다 - App") {
            // given
            val refreshToken = "valid-refresh-token"
            val deviceId = "device123"
            val clientType = "app"
            val jti = "test-jti"
            val opaqueId = "opaqueId"
            val member = mock<MemberEntity>()

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            val newJti = "new-jti"
            val accessExp = Duration.ofMinutes(30)
            val refreshExp = Duration.ofDays(7)

            // mocking
            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(newAccessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(newRefreshToken)
            whenever(jwtProvider.getJti(newRefreshToken)).thenReturn(newJti)
            whenever(jwtProperties.accessExp).thenReturn(accessExp)
            whenever(jwtProperties.refreshExp).thenReturn(refreshExp)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock<RefreshTokenEntity>())

            // when
            val result = authService.refreshAccessToken(refreshToken, deviceId, clientType)

            // then
            result.accessToken shouldBe newAccessToken
            result.refreshToken shouldBe newRefreshToken
            result.accessTokenExpiredAt shouldNotBe null
            result.refreshTokenExpiredAt shouldNotBe null

            verify(jwtProvider, times(1)).validateOrThrow(refreshToken)
            verify(jwtProvider, times(1)).getJti(refreshToken)
            verify(jwtProvider, times(1)).getOpaqueId(refreshToken)
            verify(refreshTokenRepository, times(1)).findByOpaqueIdAndJti(opaqueId, jti)
            verify(jwtProvider, times(1)).createAccessToken(any(), any(), any())
            verify(jwtProvider, times(1)).createRefreshToken(any(), any())
            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
            verify(refreshTokenRepository, times(1)).save(any<RefreshTokenEntity>())
        }

        it("유효하지 않은 refreshToken이 주어졌을 때 JWT 예외를 던져야 한다") {
            val invalidRefreshToken = "invalid-refresh-token"
            val deviceId: String? = null
            val clientType = "web"

            whenever(jwtProvider.validateOrThrow(invalidRefreshToken)).thenThrow(
                JwtException(JwtExceptionCode.INVALID_SIGNATURE)
            )

            shouldThrow<JwtException> {
                authService.refreshAccessToken(invalidRefreshToken, deviceId, clientType)
            }

            verify(jwtProvider, times(1)).validateOrThrow(invalidRefreshToken)
            verify(refreshTokenRepository, never()).findByOpaqueIdAndJti(any(), any())
        }

        it("DB에 refreshToken이 존재하지 않을 때 MemberException을 던져야 한다") {
            val refreshToken = "valid-but-not-in-db-token"
            val deviceId: String? = null
            val clientType = "web"
            val jti = "test-jti"
            val opaqueId = "opaqueId"

            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(null)

            shouldThrow<JwtException> {
                authService.refreshAccessToken(refreshToken, deviceId, clientType)
            }.code shouldBe JwtExceptionCode.MALFORMED

            verify(jwtProvider, times(1)).validateOrThrow(refreshToken)
            verify(refreshTokenRepository, times(1)).findByOpaqueIdAndJti(opaqueId, jti)
        }

        it("refreshToken이 만료되었을 때 만료된 토큰을 삭제하고 JWT 만료 예외를 던져야 한다") {
            val expiredRefreshToken = "expired-refresh-token"
            val deviceId: String? = null
            val clientType = "web"
            val jti = "test-jti"
            val opaqueId = "opaqueId"

            val pastTime = Instant.now().minusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(pastTime)

            doNothing().`when`(jwtProvider).validateOrThrow(expiredRefreshToken)
            whenever(jwtProvider.getJti(expiredRefreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(expiredRefreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)

            shouldThrow<JwtException> {
                authService.refreshAccessToken(expiredRefreshToken, deviceId, clientType)
            }.code shouldBe JwtExceptionCode.EXPIRED

            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
        }

        it("멤버가 존재하지 않을 때 NoSuchElementException을 던져야 한다") {
            val refreshToken = "valid-refresh-token"
            val deviceId: String? = null
            val clientType = "web"
            val jti = "test-jti"
            val opaqueId = "opaqueId"

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("new-access-token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("new-refresh-token")
            whenever(jwtProvider.getJti("new-refresh-token")).thenReturn("new-jti")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.empty())

            shouldThrow<NoSuchElementException> {
                authService.refreshAccessToken(refreshToken, deviceId, clientType)
            }

            verify(memberRepository, times(1)).findByOpaqueId(opaqueId)
        }

        it("App 클라이언트에서 deviceId가 필수이지만 null인 경우 예외를 던져야 한다") {
            val refreshToken = "valid-refresh-token"
            val deviceId: String? = null
            val clientType = "app"
            val jti = "test-jti"
            val opaqueId = "opaqueId"
            val member = mock<MemberEntity>()

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("new-access-token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("new-refresh-token")
            whenever(jwtProvider.getJti("new-refresh-token")).thenReturn("new-jti")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)

            // when & then
            shouldThrow<RuntimeException> {
                authService.refreshAccessToken(refreshToken, deviceId, clientType)
            }
        }

        it("Web과 App에서 다른 RefreshTokenEntity가 생성되는지 확인") {
            // given
            val refreshToken = "valid-refresh-token"
            val deviceIdForApp = "device123"
            val webClientType = "web"
            val appClientType = "app"
            val jti = "test-jti"
            val opaqueId = "opaqueId"
            val member = mock<MemberEntity>()

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            val newJti = "new-jti"

            // Common mocking
            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(newAccessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(newRefreshToken)
            whenever(jwtProvider.getJti(newRefreshToken)).thenReturn(newJti)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock<RefreshTokenEntity>())

            // when - Web client
            authService.refreshAccessToken(refreshToken, null, webClientType)

            // when - App client
            authService.refreshAccessToken(refreshToken, deviceIdForApp, appClientType)

            // then - Both should save RefreshTokenEntity
            verify(refreshTokenRepository, times(2)).save(any<RefreshTokenEntity>())
        }
    }

    it("기존 사용자가 이메일로 로그인하면 JWT 토큰을 반환한다 - App") {
        // given
        val email = "test@example.com"
        val deviceId = "device123"
        val clientType = "app"
        val memberId = 1L
        val nickname = "testUser"
        val opaqueId = "test-opaque-id"
        val accessToken = "access.token.here"
        val refreshToken = "refresh.token.here"
        val jti = "jwt-id-123"
        val expiredTime = 1800000L

        val mockMember = mock<MemberEntity>()
        val mockAuthProvider = mock<AuthProviderEntity>()

        whenever(mockMember.id).thenReturn(memberId)
        whenever(mockMember.nickname).thenReturn(nickname)
        whenever(mockMember.opaqueId).thenReturn(opaqueId)
        whenever(mockAuthProvider.email).thenReturn(email)
        whenever(mockAuthProvider.member).thenReturn(mockMember)

        whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
        whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
        whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
        whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
        whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
        whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
        whenever(refreshTokenRepository.findByMemberAndDeviceId(mockMember, deviceId)).thenReturn(null)
        whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

        // when
        val result = authService.emailLogin(email, deviceId, clientType)

        // then
        result.accessToken shouldBe accessToken
        result.refreshToken shouldBe refreshToken
        result.accessTokenExpiredAt shouldNotBe null
        result.refreshTokenExpiredAt shouldNotBe null

        verify(authProviderRepository).findByEmail(email)
        verify(jwtProvider).createAccessToken(any(), any(), any())
        verify(jwtProvider).createRefreshToken(any(), any())
        verify(refreshTokenRepository).save(any<RefreshTokenEntity>())
    }

    it("기존 사용자 로그인 시 이전 refreshToken이 있으면 삭제 후 새로 생성한다") {
        // given
        val email = "test@example.com"
        val deviceId = "device123"
        val clientType = "app"
        val memberId = 1L
        val opaqueId = "opaqueId"
        val mockMember = mock<MemberEntity>()
        val mockAuthProvider = mock<AuthProviderEntity>()
        val existingRefreshToken = mock<RefreshTokenEntity>()

        whenever(mockMember.id).thenReturn(memberId)
        whenever(mockMember.opaqueId).thenReturn(opaqueId)
        whenever(mockAuthProvider.member).thenReturn(mockMember)
        whenever(mockAuthProvider.email).thenReturn(email)

        whenever(authProviderRepository.findByEmail(email)).thenReturn(mockAuthProvider)
        whenever(refreshTokenRepository.findByMemberAndDeviceId(mockMember, deviceId)).thenReturn(existingRefreshToken)
        whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("refresh-token")
        whenever(jwtProvider.getJti("refresh-token")).thenReturn("jti")
        whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
        whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))

        // when
        authService.emailLogin(email, deviceId, clientType)

        // then
        verify(refreshTokenRepository).delete(existingRefreshToken)
        verify(refreshTokenRepository).save(any<RefreshTokenEntity>())
    }

    it("존재하지 않는 이메일로 로그인하면 회원가입을 진행한다") {
        // given
        val email = "newuser@example.com"
        val deviceId: String? = null
        val clientType = "web"
        val memberId = 2L
        val nickname = "newUser123"
        val opaqueId = "new-opaque-id"
        val accessToken = "new.access.token"
        val refreshToken = "new.refresh.token"
        val jti = "new-jwt-id"
        val expiredTime = 1800000L

        val mockMember = mock<MemberEntity>()

        whenever(mockMember.id).thenReturn(memberId)
        whenever(mockMember.nickname).thenReturn(nickname)
        whenever(mockMember.opaqueId).thenReturn(opaqueId)

        whenever(authProviderRepository.findByEmail(email)).thenReturn(null)
        whenever(nicknameGenerator.generate()).thenReturn(nickname)
        whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
        whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
        whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
        whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
        whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
        whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
        whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
        whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
        whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

        // when
        val result = authService.emailLogin(email, deviceId, clientType)

        // then
        result.accessToken shouldBe accessToken
        result.refreshToken shouldBe refreshToken
        result.accessTokenExpiredAt shouldNotBe null
        result.refreshTokenExpiredAt shouldNotBe null

        verify(authProviderRepository).findByEmail(email)
        verify(nicknameGenerator).generate()
        verify(memberRepository).existsByNickname(nickname)
        verify(memberRepository).save(any<MemberEntity>())
        verify(authProviderRepository).save(any<AuthProviderEntity>())
    }

    describe("회원가입 테스트") {

        it("새로운 이메일로 회원가입하면 계정을 생성하고 JWT 토큰을 반환한다 - Web") {
            // given
            val email = "signup@example.com"
            val deviceId: String? = null
            val clientType = "web"
            val memberId = 3L
            val nickname = "signupUser456"
            val opaqueId = "signup-opaque-id"
            val accessToken = "signup.access.token"
            val refreshToken = "signup.refresh.token"
            val jti = "signup-jwt-id"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            val result = authService.signUp(email, deviceId, clientType)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.accessTokenExpiredAt shouldNotBe null
            result.refreshTokenExpiredAt shouldNotBe null

            verify(authEmailService).validateEmailFormat(email)
            verify(nicknameGenerator).generate()
            verify(memberRepository).existsByNickname(nickname)
            verify(memberRepository).save(any<MemberEntity>())
            verify(authProviderRepository).save(any<AuthProviderEntity>())
            verify(jwtProvider).createAccessToken(any(), any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
        }

        it("새로운 이메일로 회원가입하면 계정을 생성하고 JWT 토큰을 반환한다 - App") {
            // given
            val email = "signup@example.com"
            val deviceId = "device456"
            val clientType = "app"
            val memberId = 3L
            val nickname = "signupUser456"
            val opaqueId = "signup-opaque-id"
            val accessToken = "signup.access.token"
            val refreshToken = "signup.refresh.token"
            val jti = "signup-jwt-id"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(nickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            val result = authService.signUp(email, deviceId, clientType)

            // then
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
            result.accessTokenExpiredAt shouldNotBe null
            result.refreshTokenExpiredAt shouldNotBe null

            verify(authEmailService).validateEmailFormat(email)
            verify(nicknameGenerator).generate()
            verify(memberRepository).existsByNickname(nickname)
            verify(memberRepository).save(any<MemberEntity>())
            verify(authProviderRepository).save(any<AuthProviderEntity>())
            verify(jwtProvider).createAccessToken(any(), any(), any())
            verify(jwtProvider).createRefreshToken(any(), any())
        }

        it("중복된 닉네임이 생성되면 새로운 닉네임을 재생성한다") {
            // given
            val email = "duplicate@example.com"
            val deviceId: String? = null
            val clientType = "web"
            val memberId = 4L
            val duplicateNickname = "duplicate123"
            val uniqueNickname = "unique456"
            val opaqueId = "duplicate-opaque-id"
            val accessToken = "duplicate.access.token"
            val refreshToken = "duplicate.refresh.token"
            val expiredTime = 1800000L

            val mockMember = mock<MemberEntity>()

            whenever(mockMember.id).thenReturn(memberId)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)

            whenever(nicknameGenerator.generate())
                .thenReturn(duplicateNickname)
                .thenReturn(uniqueNickname)
            whenever(memberRepository.existsByNickname(duplicateNickname)).thenReturn(true)
            whenever(memberRepository.existsByNickname(uniqueNickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(accessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn("jti")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(expiredTime))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            val result = authService.signUp(email, deviceId, clientType)

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
            val deviceId: String? = null
            val clientType = "web"
            val duplicateNickname = "duplicate"
            val uniqueNickname = "unique"
            val opaqueId = "test-opaque-id"

            val mockMember = mock<MemberEntity>()
            whenever(mockMember.id).thenReturn(1L)
            whenever(mockMember.nickname).thenReturn(uniqueNickname)
            whenever(mockMember.opaqueId).thenReturn(opaqueId)

            whenever(nicknameGenerator.generate())
                .thenReturn(duplicateNickname, duplicateNickname, duplicateNickname, uniqueNickname)
            whenever(memberRepository.existsByNickname(duplicateNickname)).thenReturn(true)
            whenever(memberRepository.existsByNickname(uniqueNickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenReturn(mockMember)
            whenever(authProviderRepository.save(any<AuthProviderEntity>())).thenReturn(mock())
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("refresh")
            whenever(jwtProvider.getJti("refresh")).thenReturn("jti")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMinutes(30))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            authService.signUp(email, deviceId, clientType)

            // then
            verify(nicknameGenerator, times(4)).generate()
        }

        it("잘못된 이메일 형식으로 회원가입 시 예외가 발생한다") {
            // given
            val invalidEmail = "invalid-email"
            val deviceId: String? = null
            val clientType = "web"
            val exception = RuntimeException("Invalid email format")

            whenever(authEmailService.validateEmailFormat(invalidEmail)).thenThrow(exception)

            // when & then
            shouldThrow<RuntimeException> {
                authService.signUp(invalidEmail, deviceId, clientType)
            }

            verify(authEmailService).validateEmailFormat(invalidEmail)
            verifyNoInteractions(nicknameGenerator)
            verifyNoInteractions(memberRepository)
        }

        it("회원가입시 Member 엔티티의 속성이 올바르게 설정된다") {
            // given
            val email = "entity@example.com"
            val deviceId: String? = null
            val clientType = "web"
            val nickname = "entityUser"
            val opaqueId = "entity-opaque-id"

            val memberCaptor = argumentCaptor<MemberEntity>()
            val providerCaptor = argumentCaptor<AuthProviderEntity>()

            val mockMember = mock<MemberEntity>()
            whenever(mockMember.opaqueId).thenReturn(opaqueId)

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(memberCaptor.capture())).thenAnswer { mockMember }
            whenever(authProviderRepository.save(providerCaptor.capture())).thenAnswer { it.arguments[0] as AuthProviderEntity }
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("token")
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn("refresh")
            whenever(jwtProvider.getJti("refresh")).thenReturn("jti")
            whenever(jwtProperties.accessExp).thenReturn(Duration.ofMillis(1800000L))
            whenever(jwtProperties.refreshExp).thenReturn(Duration.ofDays(7))
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock())

            // when
            authService.signUp(email, deviceId, clientType)

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
            val deviceId: String? = null
            val clientType = "web"
            val nickname = "testUser"
            val dbException = RuntimeException("Database connection failed")

            whenever(nicknameGenerator.generate()).thenReturn(nickname)
            whenever(memberRepository.existsByNickname(nickname)).thenReturn(false)
            whenever(memberRepository.save(any<MemberEntity>())).thenThrow(dbException)

            // when & then
            val exception = shouldThrow<AuthException> {
                authService.signUp(email, deviceId, clientType)
            }

            exception.code shouldBe AuthExceptionCode.DATABASE_SAVE_FAILED
            exception.cause shouldBe dbException
        }
    }

    describe("로그아웃 테스트") {

        it("성공 - refreshToken 삭제 호출") {
            // given
            val refreshToken = "dummyToken"
            val opaqueId = "opaqueId"
            val jti = "jti123"

            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)

            // when
            authService.logout(refreshToken)

            // then
            verify(refreshTokenRepository, times(1)).deleteByOpaqueIdAndJti(opaqueId, jti)
        }

        it("실패 - DB 삭제 예외 발생 시 MemberException 던짐") {
            // given
            val refreshToken = "dummyToken"
            val opaqueId = "opaqueId"
            val jti = "jti123"

            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(refreshTokenRepository.deleteByOpaqueIdAndJti(opaqueId, jti))
                .thenThrow(RuntimeException("DB error"))

            // when
            val ex = shouldThrow<AuthException> {
                authService.logout(refreshToken)
            }

            // then
            ex.code shouldBe AuthExceptionCode.LOGOUT_FAILED
            verify(refreshTokenRepository, times(1)).deleteByOpaqueIdAndJti(opaqueId, jti)
        }
    }

    describe("AccessToken 재발급") {
        it("유효한 refreshToken이 주어졌을 때 새로운 토큰들을 생성하고 반환해야 한다 - Web") {
            // given
            val refreshToken = "valid-refresh-token"
            val deviceId: String? = null
            val clientType = "web"
            val jti = "test-jti"
            val opaqueId = "opaqueId"
            val member = mock<MemberEntity>()

            val futureTime = Instant.now().plusSeconds(3600)
            val refreshTokenEntity = mock<RefreshTokenEntity>()
            whenever(refreshTokenEntity.expiredAt).thenReturn(futureTime)

            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            val newJti = "new-jti"
            val accessExp = Duration.ofMinutes(30)
            val refreshExp = Duration.ofDays(7)

            // mocking
            doNothing().`when`(jwtProvider).validateOrThrow(refreshToken)
            whenever(jwtProvider.getJti(refreshToken)).thenReturn(jti)
            whenever(jwtProvider.getOpaqueId(refreshToken)).thenReturn(opaqueId)
            whenever(refreshTokenRepository.findByOpaqueIdAndJti(opaqueId, jti)).thenReturn(refreshTokenEntity)
            whenever(jwtProvider.createAccessToken(any(), any(), any())).thenReturn(newAccessToken)
            whenever(jwtProvider.createRefreshToken(any(), any())).thenReturn(newRefreshToken)
            whenever(jwtProvider.getJti(newRefreshToken)).thenReturn(newJti)
            whenever(jwtProperties.accessExp).thenReturn(accessExp)
            whenever(jwtProperties.refreshExp).thenReturn(refreshExp)
            whenever(memberRepository.findByOpaqueId(opaqueId)).thenReturn(Optional.of(member))
            doNothing().`when`(refreshTokenRepository).delete(refreshTokenEntity)
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(mock<RefreshTokenEntity>())

            // when
            val result = authService.refreshAccessToken(refreshToken, deviceId, clientType)

            // then
            result.accessToken shouldBe newAccessToken
            result.refreshToken shouldBe newRefreshToken
            result.accessTokenExpiredAt shouldNotBe null
            result.refreshTokenExpiredAt shouldNotBe null

            verify(jwtProvider, times(1)).validateOrThrow(refreshToken)
            verify(jwtProvider, times(1)).getJti(refreshToken)
            verify(jwtProvider, times(1)).getOpaqueId(refreshToken)
            verify(refreshTokenRepository, times(1)).findByOpaqueIdAndJti(opaqueId, jti)
            verify(jwtProvider, times(1)).createAccessToken(any(), any(), any())
            verify(jwtProvider, times(1)).createRefreshToken(any(), any())
            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
            verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity)
            verify(refreshTokenRepository, times(1)).save(any<RefreshTokenEntity>())
            verifyNoInteractions(authEmailService)
        }

    }
})