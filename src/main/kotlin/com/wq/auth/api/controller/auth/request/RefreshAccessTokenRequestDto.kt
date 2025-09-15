package com.wq.auth.api.controller.auth.request

data class RefreshAccessTokenRequestDto (val refreshToken: String?, val deviceId: String?)