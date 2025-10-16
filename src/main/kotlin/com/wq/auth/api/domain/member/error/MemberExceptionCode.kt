package com.wq.auth.api.domain.member.error

import com.wq.auth.shared.error.ApiResponseCode

enum class MemberExceptionCode (
    override val status: Int,
    override val message: String
) : ApiResponseCode {
    USER_INFO_RETRIEVE_FAILED(500, "회원 정보를 조회하는데 실패했습니다."),
    MEMBER_NOT_FOUND(404, "회원을 찾을 수 없습니다"),
}