package com.wq.auth.shared.utils

object VerificationCodeGenerator {
    fun generateRandomCode(): String{
        val code = (100000..999999).random().toString() // 6자리
        return code
    }

}