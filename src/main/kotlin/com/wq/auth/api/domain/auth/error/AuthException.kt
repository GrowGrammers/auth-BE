package com.wq.auth.api.domain.auth.error

import com.wq.auth.shared.error.ApiException

class AuthException(
    val authCode: AuthExceptionCode,
    cause: Throwable? = null
) : ApiException(authCode, cause)