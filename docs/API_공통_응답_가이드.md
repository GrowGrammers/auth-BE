## API 공통 응답 가이드 (`ApiCode` 기반)

### 1. 개요

이번 작업에서는 API 응답을 **일관성 있게 관리**하기 위해
`ApiCode`, `ApiException`, `BaseResponse`, `Responses` 구조를 추가했습니다.

---

### 2. 구성 요소

#### **2.1 ApiCode (Interface)**

```kotlin
interface ApiCode {
    fun getStatus(): Int        // HTTP Status Code
    fun getMessage(): String?   // 사용자 표시 메시지
}
```

* 모든 API 응답 코드(성공/실패)를 한 곳에서 관리
* Enum으로 구현하여 **코드 재사용성**과 **타입 안전성** 확보

#### **2.2 ApiException**

```kotlin
class ApiException(val code: ApiCode) : RuntimeException(code.getMessage()) {
    val className: String
    val methodName: String
    val lineNumber: Int
}
```

* `ApiResponseCode` 기반 예외
* 발생 위치(클래스, 메서드, 라인)를 함께 기록 → **로그 분석 편의성**

#### **2.3 BaseResponse & Responses**

```kotlin
/**
 * 공통 API 응답 구조
 */
data class BaseResponse<T> (
    val success: Boolean,
    val message: String,
    val data: T?,
    val error: String? = null
)

/**
 * 공통 success, fail 메서드.
 */
object Responses {
    fun <T> success(
        message: String = "요청에 성공적으로 응답하였습니다.",
        data: T? = null
    ) : BaseResponse<T> =
        BaseResponse(true, message, data, null)

    fun fail(code: ApiCode) : BaseResponse<Nothing> =
        BaseResponse(false, code.getMessage() ?: "오류가 발생했습니다.", null, code.toString())
}
```

* 성공/실패 응답 구조 통일

---

### 3. 사용 예시

#### **3.1 ApiCode Enum 예시**

```kotlin
enum class JwtResponseCode(
    private val status: Int,
    private val msg: String
) : ApiResponseCode {

    TOKEN_ISSUED_SUCCESS(200, "토큰이 발급되었습니다"), // 성공 응답
    
    TOKEN_EXPIRED(401, "토큰이 만료되었습니다"),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다"),
    TOKEN_MISSING(401, "토큰이 누락되었습니다");

    override fun getStatus() = status
    override fun getMessage() = msg
}
```

* 상속받아서 재활용해서 각 클래스에 맞게 code 변환해서 사용.

#### **3.2 컨트롤러에서 사용**

```kotlin
@GetMapping("/hello")
fun hello(): BaseResponse<String> {
    return Responses.success(message = "Hello World!")
}
```

#### **3.3 예외 발생 시**

```kotlin
@GetMapping("/secure")
fun secure(): BaseResponse<String> {
    throw ApiException(CommonCode.INVALID_TOKEN)
}
```

* code만 매핑 해주면, 해당 메세지로 에러 응답 반환.

---

### 4. 유의사항

1. **성공 응답도 `ApiResponseCode`상속해서 관리**

    * 성공 코드도 `ApiResponseCode` 상속받은 enum 클래스에서 정의하면
      메시지와 상태 코드를 한 곳에서 관리할 수 있음.

2. **`ApiException`은 서비스/도메인 계층에서 던지고, 컨트롤러 레벨에서 잡지 않음**

    * `GlobalExceptionHandler`에서 처리하도록 일원화.

3. **Swagger 연동 시**

    * `BaseResponse`를 응답 타입으로 지정하면 문서에서 일관성 유지.

4. **로그 분석**

    * `ApiException.extractExceptionLocation()`을 사용하면 예외 발생 지점을 간결하게 로깅 가능.
