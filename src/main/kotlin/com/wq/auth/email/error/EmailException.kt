package com.wq.auth.email.error

import com.wq.auth.shared.error.ApiException

class EmailException(
    val emailCode: EmailExceptionCode,
    cause: Throwable? = null
) : ApiException(emailCode, cause)