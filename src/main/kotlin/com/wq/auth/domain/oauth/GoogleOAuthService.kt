package com.wq.auth.domain.oauth

import com.fasterxml.jackson.databind.ObjectMapper
import com.wq.auth.api.controller.oauth.dto.GoogleUserInfoDto
import com.wq.auth.domain.oauth.error.SocialLoginException
import com.wq.auth.domain.oauth.error.SocialLoginExceptionCode
import com.wq.auth.domain.oauth.properties.GoogleOAuthProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

/**
 * Google OAuth2 서비스
 * 
 * Google OAuth2 API와 통신하여 인가 코드를 액세스 토큰으로 교환하고,
 * 액세스 토큰을 사용하여 사용자 정보를 조회합니다.
 */
@Service
class GoogleOAuthService(
    private val googleOAuthProperties: GoogleOAuthProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}
    private val restTemplate = RestTemplate()

    /**
     * 인가 코드를 사용하여 액세스 토큰을 획득합니다.
     * 
     * @param authorizationCode Google로부터 받은 인가 코드
     * @param redirectUri 리다이렉트 URI (선택사항)
     * @return Google 액세스 토큰
     * @throws SocialLoginException 토큰 획득 실패 시
     */
    fun getAccessToken(authorizationCode: String, redirectUri: String? = null): String {
        log.info { "Google 액세스 토큰 요청 시작" }
        log.info { "redirectUri: "+ googleOAuthProperties.redirectUri }
        
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }
        
        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("client_id", googleOAuthProperties.clientId)
            add("client_secret", googleOAuthProperties.clientSecret)
            add("code", authorizationCode)
            add("grant_type", "authorization_code")
            add("redirect_uri", redirectUri ?: googleOAuthProperties.redirectUri)
        }
        
        val request = HttpEntity(body, headers)
        
        try {
            val response = restTemplate.postForEntity(
                googleOAuthProperties.tokenUri,
                request,
                String::class.java
            )
            
            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val tokenResponse = objectMapper.readTree(response.body!!)
                val accessToken = tokenResponse.get("access_token")?.asText()
                
                if (accessToken != null) {
                    log.info { "Google 액세스 토큰 획득 성공" }
                    return accessToken
                } else {
                    log.error { "Google 액세스 토큰이 응답에 없습니다: ${response.body}" }
                    throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_TOKEN_REQUEST_FAILED)
                }
            } else {
                log.error { "Google 토큰 요청 실패: ${response.statusCode}" }
                throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_TOKEN_REQUEST_FAILED)
            }
            
        } catch (e: HttpClientErrorException) {
            log.error(e) { "Google 토큰 요청 클라이언트 오류: ${e.statusCode} - ${e.responseBodyAsString}" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_INVALID_AUTHORIZATION_CODE, e)
        } catch (e: HttpServerErrorException) {
            log.error(e) { "Google 서버 오류: ${e.statusCode}" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_SERVER_ERROR, e)
        } catch (e: SocialLoginException) {
            throw e // 이미 SocialLoginException인 경우 그대로 전파
        } catch (e: Exception) {
            log.error(e) { "Google 토큰 요청 중 예상치 못한 오류 발생" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_TOKEN_REQUEST_FAILED, e)
        }
    }

    /**
     * 액세스 토큰을 사용하여 Google 사용자 정보를 조회합니다.
     * 
     * @param accessToken Google 액세스 토큰
     * @return Google 사용자 정보
     * @throws SocialLoginException 사용자 정보 조회 실패 시
     */
    fun getUserInfo(accessToken: String): GoogleUserInfoDto {
        log.info { "Google 사용자 정보 조회 시작" }
        
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
        }
        
        val request = HttpEntity<String>(headers)
        
        try {
            val response = restTemplate.exchange(
                googleOAuthProperties.userInfoUri,
                HttpMethod.GET,
                request,
                String::class.java
            )
            
            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val userInfo = objectMapper.readValue(response.body!!, GoogleUserInfoDto::class.java)
                log.info { "Google 사용자 정보 조회 성공: ${userInfo.email}" }
                return userInfo
            } else {
                log.error { "Google 사용자 정보 조회 실패: ${response.statusCode}" }
                throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_USER_INFO_REQUEST_FAILED)
            }
            
        } catch (e: HttpClientErrorException) {
            log.error(e) { "Google 사용자 정보 조회 클라이언트 오류: ${e.statusCode}" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_INVALID_ACCESS_TOKEN, e)
        } catch (e: HttpServerErrorException) {
            log.error(e) { "Google 서버 오류: ${e.statusCode}" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_SERVER_ERROR, e)
        } catch (e: SocialLoginException) {
            throw e // 이미 SocialLoginException인 경우 그대로 전파
        } catch (e: Exception) {
            log.error(e) { "Google 사용자 정보 조회 중 예상치 못한 오류 발생" }
            throw SocialLoginException(SocialLoginExceptionCode.GOOGLE_USER_INFO_REQUEST_FAILED, e)
        }
    }

    /**
     * 인가 코드를 사용하여 사용자 정보를 직접 조회합니다.
     * 
     * @param authorizationCode Google로부터 받은 인가 코드
     * @param redirectUri 리다이렉트 URI (선택사항)
     * @return Google 사용자 정보
     */
    fun getUserInfoFromAuthCode(authorizationCode: String, redirectUri: String? = null): GoogleUserInfoDto {
        val accessToken = getAccessToken(authorizationCode, redirectUri)
        return getUserInfo(accessToken)
    }
}

