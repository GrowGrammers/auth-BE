package com.wq.demo.email.error

import com.wq.demo.shared.error.ApiResponseCode

enum class EmailExceptionCode (
    override val status: Int,
    override val message: String
) : ApiResponseCode {
    INVALID_EMAIL_FORMAT(400, "올바르지 않은 이메일 형식입니다."),
    EMAIL_VERIFICATION_FAILED(401, "이메일 인증코드가 일치하지 않습니다."),
    EMAIL_NOT_SENDED(500,"이메일 인증코드 전송에 실패했습니다."),
    EMAIL_SERVER_NOT_FOUND(400,"해당 도메인에 메일을 보낼 수 없습니다." ),
    DOMAIN_NOT_FOUND(400,"존재하지 않는 도메인입니다.");
}