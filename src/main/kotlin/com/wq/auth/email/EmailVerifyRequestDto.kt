package com.wq.auth.email

data class EmailVerifyRequestDto (val email: String, val code: String)