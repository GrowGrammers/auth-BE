package com.wq.demo.shared.error

interface ApiCode {
    fun getStatus(): Int // HTTP status code (e.g., 200, 404, 401)
    fun getMessage(): String // 사용자 표시용 메시지
}