package com.wq.demo.email.error

import com.wq.demo.shared.error.ApiException

class EmailException(
    val emailCode: EmailExceptionCode,
    cause: Throwable? = null
) : ApiException(emailCode, cause)