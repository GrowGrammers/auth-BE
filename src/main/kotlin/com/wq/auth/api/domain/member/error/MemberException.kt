package com.wq.auth.api.domain.member.error

import com.wq.auth.api.domain.member.error.MemberExceptionCode
import com.wq.auth.shared.error.ApiException

class MemberException(
    val memberCode: MemberExceptionCode,
    cause: Throwable? = null
) : ApiException(memberCode, cause)