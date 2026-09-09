# auth-api

Kotlin / Spring Boot 기반 인증 API입니다. 소셜 로그인(Google, Kakao, Naver), 이메일 인증, JWT·쿠키, `GET /api/v1/auth/introspect` 등을 제공합니다.

## 문서

- [API 명세](docs/api-명세서.md)
- [GitHub Environments / 배포 CI](docs/GITHUB-ENVIRONMENTS.md)

## 실행

```bash
./gradlew bootRun
```

기본 포트 **9000**, 프로필은 `application.yml`의 `spring.profiles` 그룹을 따릅니다.

## 환경 변수

`application.yml`, `application-jwt.yml`, `application-oauth.yml`, 프로필별 `application-*.yml`에 매핑됩니다.  
필요한 키는 위 API 명세와 설정 파일을 참고하세요.

### JWT 서명 키

AT·RT 는 **RS256** 으로 서명합니다. 개인키는 auth-api 만 가지고, 공개키는 `GET /.well-known/jwks.json` 으로 배포합니다.

| 변수 | 필수 | 설명 |
|---|---|---|
| `JWT_PRIVATE_KEY` | 예 | PKCS#8 RSA 개인키. PEM 헤더·푸터·개행을 뺀 **한 줄 base64** |
| `JWT_SECRET` | 아니오 | HS256 **레거시 검증 전용** 시크릿. HS256 으로 발급된 토큰이 살아 있는 전환 기간(RT 만료 기간)에만 두고, 지나면 지웁니다. 비어 있으면 HS256 토큰을 거부합니다 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 예 | 예: `30m` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | 예 | 예: `7d` |

키 생성 (환경마다 1회):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
grep -v '^-----' jwt-private.pem | tr -d '\n'     # 이 한 줄이 JWT_PRIVATE_KEY 값
```

HS256 → RS256 전환 절차: `JWT_PRIVATE_KEY` 를 **추가**하고 `JWT_SECRET` 은 **유지**한 채 배포한다(세션 유지). `JWT_REFRESH_TOKEN_EXPIRATION` 이상 지난 뒤 `JWT_SECRET` 을 지우고 재배포한다.

전환 시 유의:

- `JWT_SECRET` 을 너무 일찍 지우면: 옛 HS256 AT 를 가진 브라우저는 introspect 에서 쿠키가 지워지지 않은 채 401 을 받고, AT 쿠키 만료(`JWT_ACCESS_TOKEN_EXPIRATION`) 뒤 silent refresh 경로에서 쿠키가 지워져 재로그인하게 됩니다. `JWT_REFRESH_TOKEN_EXPIRATION` 이상 기다리면 이 상황이 생기지 않습니다.
- 롤백: RS256 이미지에서 HS256 이미지로 되돌리면 그 사이 발급된 세션은 전부 깨집니다. 롤백 가능성을 남기려면 전환 창 동안 `JWT_SECRET` 을 유지합니다.
- 키 교체(`JWT_PRIVATE_KEY` 변경)는 `kid` 가 바뀌어 기존 세션 전원 로그아웃이며, JWKS 를 캐시하는 소비자(lnb-api)도 재시작해야 합니다. 회전 지원은 이번 범위 밖입니다.
- 토큰 크기: RS256 서명은 HS256 보다 약 300 바이트 깁니다(AT·RT 각각). 쿠키·`X-New-AT`/`X-New-RT` 헤더 크기가 그만큼 늘어납니다.
