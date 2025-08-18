package com.wq.demo.shared.error

interface ApiResponseCode {
    val status: Int    // HTTP status code (e.g., 200, 404, 401)
    val message: String // 사용자 표시용 메시지
}