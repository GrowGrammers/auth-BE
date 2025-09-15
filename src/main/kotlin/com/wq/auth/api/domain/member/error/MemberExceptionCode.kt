package com.wq.auth.api.domain.member.error

import com.wq.auth.shared.error.ApiResponseCode

enum class MemberExceptionCode (
    override val status: Int,
    override val message: String
) : ApiResponseCode {
    DATABASE_SAVE_FAILED(500, "회원 정보를 저장하는데 실패했습니다."),
    LOGOUT_FAILED(500, "로그아웃에 실패했습니다."),
}