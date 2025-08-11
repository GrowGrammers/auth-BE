package com.wq.demo.shared.error

/**
 * API 처리 중 발생하는 표준 예외.
 * ApiCode를 통해 HTTP 상태코드와 메시지를 함께 보관.
 */
open class ApiException(
    val code: ApiResponseCode,
    cause: Throwable? = null
) : RuntimeException(code.getMessage(), cause) {

    val className: String
    val methodName: String
    val lineNumber: Int

    init {
        val stack = Thread.currentThread().stackTrace
        // [0] = getStackTrace, [1] = init, [2] = 생성자 호출 위치
        className = stack[2].className
        methodName = stack[2].methodName
        lineNumber = stack[2].lineNumber
    }

    fun extractExceptionLocation(): String {
        val simpleClassName = className.substringAfterLast('.')
        return "[$simpleClassName.$methodName():line:$lineNumber] "
    }
}
