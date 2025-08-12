package com.wq.demo.web.common

import com.wq.demo.shared.error.ApiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<BaseResponse<Nothing>> {

        log.error(e.extractExceptionLocation() + e.message)
        val status = HttpStatus.valueOf(e.code.getStatus())
        val body = Responses.fail(e.code)
        return ResponseEntity.status(status).body(body)
    }

    // 예상 못 한 예외 처리
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<BaseResponse<Nothing>> {
        log.error("[예상치 못한 예외 발생] $e")
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val body = BaseResponse(
            success = false,
            message = e.message ?: "Internal server error",
            data = null
        )
        return ResponseEntity.status(status).body(body)
    }
}