package com.wq.auth.api.domain.member.error

import com.wq.auth.shared.error.ApiResponseCode

enum class MemberExceptionCode (
    override val status: Int,
    override val message: String
) : ApiResponseCode {

}