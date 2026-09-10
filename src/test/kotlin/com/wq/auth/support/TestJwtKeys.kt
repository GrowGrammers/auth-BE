package com.wq.auth.support

import com.wq.auth.security.jwt.JwtKeys

/**
 * 테스트 전용 RSA-2048 개인키. 운영 키가 아니다.
 *
 * `@SpringBootTest(properties = [...])` 는 컴파일 타임 상수만 받으므로 `const val` 로 둔다.
 * `"jwt.private-key=" + TestJwtKeys.PRIVATE_KEY_B64` 처럼 이어 붙여 쓴다.
 *
 * 형식은 운영과 같다 — PKCS#8 PEM 에서 헤더·푸터·개행을 뺀 한 줄 base64.
 */
object TestJwtKeys {
    const val PRIVATE_KEY_B64: String =
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDEMaTboxD4/0/t732hV/qh7x9G602anNuS7M+RwwtUdhEUupuuNkgnOa3W3UGFvDX2m+z+Foncihp3KpjZ+Vrh79YlmSDAXGkpiybinM8b06XNijhdo3mxB3YWFQL/TTEfnQHwoCktiO8DnX/rVZCKC3KTYP2rO8XrEEFf+1bHWXp2u4uASNY7Omd/RiWXz17pbY0Qp9SjcylFK6n8gmrMCl62axwRpFsVKRvQOjt8U7AVJ/uQhJbfzfxqdHualJTJW9Z/EcNvOssRJ7GdyXe6uJ4YbfelFP1HZRR+9p1Ws0RVq5hpvGqQh5QPWLmAODAcKdtWBijdjaHKN9Ko0PrxAgMBAAECggEAPFGbAt/2luc/9sI72MAI0YKIFjC/0jpJk7l087aIpDYMeFgLBxuvQUgNd1LdEzXTTXeQ/GmHhyN6kYqqJdHxLCelqjeTBzEiL+CVWvSlBEQk5LWLMNbS0ieeGZnqKv3bjYGjGAzVvCcufOkObBcJcjekyUT3mI6vAd8kilLm0jDZbSbVtA8S2EtdwRW49E+4O3I7Q7hOu2ZlYlczcosCj8zG8cxe3KNghC+onuQswoKElYeFGVsQBALE6K2CtowcxImrsph79P3jmxCf46GNV4A5r7HJi7LMhDYzQXX2xZiYFwyS6XiKy9jvawyJdxMi9Nxs0YbtpjdyHw3A6MEQBQKBgQDq+ugqooZ4FAPCCadZvJY1UVX7L1sxtOBJge20o++3IJeRDnvLxqXfUpEUi+G0g80+lO6ljOUdpuTOFNN1gigY01JNQuFDU7G4sBnLEocBLz6lVNOCcY67yWYKvVnU+4LjKcEjwUVlQAdM59+HfJ4gMSPgNg8g/lIvBDEmmMnvMwKBgQDVvoaDTJC97yEfc1CH6GChjk2VfS/KIbLK6qJFEAUMccMKdNr2yJ8aOgwbXB6/bLZHHYCKnakWNWgsBDUzh2t9PG6XZe/ZPgHKVyQl8L6LNzuZTBDBFYkS3QkryRfUuV56grKXPc98FD44L//QjmmqyJZHlhO62bEE8TNOdLR9SwKBgB0cPlzhy5TereSA+6mDUnyCegtnP3318X9JyGADmzPtprlCuRVyo6P5/50zAyAw3+Fr4/DAdrXoshnRvKynFj6VF3IY4f1MRg0iS9+7iNwdtT4YNyfG167t8WVNNe7LxdhMmO/MBsPXXdAqPaf0SUalr9Mb/13QA/BtLYWhkgq/AoGALGDVftdbygOnRedkVgN6ZLCuDRaj4HzkqVrT/DDaS34nN7mRaOG4nvJkZx3WSHpi0hsfACjB3ZmTGmh5P0yjlaoBcC+6/8jvCDrVh4cXmMIL/sUbSWuWPTNlXxDugL1ID3mpaNttg96UGIhmvb+D0vC4uv4+9HGUXmlR9L0oaEcCgYAZaCMIwQTiSxmChjK2S4LFi81cpDyc1bTiT6fSIp+8dDA+51pIBzxeiezyD3mDQy1TitnMp1hd0voUWViX3Yvuf+ud8XuyR+rWEAzZhqeDQvvWRlSKO0uLJPzBoKO8jhaE+7F8tmlUwK3v1TR6gJPqM8ODhRN7LH9B1R715QknSg=="

    /** 기존 테스트가 HS256 에 쓰던 32바이트 시크릿 ("0123456789…" ASCII) 의 base64. */
    const val LEGACY_SECRET_B64: String = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="

    fun keys(withLegacy: Boolean = false): JwtKeys =
        JwtKeys(PRIVATE_KEY_B64, if (withLegacy) LEGACY_SECRET_B64 else "")
}
