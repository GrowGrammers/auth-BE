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

#### 키 발급과 시크릿 등록 (환경마다 1회)

**로컬 터미널에서** 실행합니다. EC2 에 들어갈 필요가 없고, 서버에는 키 파일을 남기지 않습니다.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
grep -v '^-----' jwt-private.pem | tr -d '\n' | pbcopy   # JWT_PRIVATE_KEY 값이 클립보드에 복사된다
```

- `genpkey` 는 PKCS#8(`BEGIN PRIVATE KEY`)로 나옵니다. `openssl genrsa` 로 만든 PKCS#1(`BEGIN RSA PRIVATE KEY`)은 읽지 못하므로 `openssl pkcs8 -topk8 -nocrypt -in rsa.pem -out jwt-private.pem` 으로 변환합니다.
- PEM 본문이 이미 base64 이므로 **`base64` 명령으로 한 번 더 감싸지 않습니다.** 두 번 감싸면 "PKCS#8 RSA 개인키가 아닙니다" 로 기동이 실패합니다.
- 한 줄이어야 합니다. `.env` 는 여러 줄 값을 담지 못합니다. 값 양 끝에 따옴표를 붙이지 않습니다.
- 2048비트 미만 키는 기동 시 거부됩니다.

등록: GitHub 저장소 **Settings → Environments → (alpha | production) → `AUTH_BE_ENV_FILE`** 을 열어 아래 줄을 추가합니다. 환경마다 키를 따로 만듭니다. 배포 워크플로가 이 시크릿을 EC2 의 `env/auth-api.env` 로 복사하므로 서버에서 손댈 것은 없습니다.

```
JWT_PRIVATE_KEY=MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQD...   # 한 줄
```

- 청첩장(alpha·prod): `JWT_SECRET` 줄은 그대로 두고 이 줄만 추가합니다(아래 전환 절차).
- lnb: `JWT_PRIVATE_KEY` 만 넣고 `JWT_SECRET` 은 넣지 않습니다.
- 등록이 끝나면 로컬의 `jwt-private.pem` 을 삭제합니다. 저장소에 커밋하지 않습니다. 테스트 코드의 `TestJwtKeys` 키는 테스트 전용이라 운영에 쓰면 안 됩니다.

#### 배포 후 확인

키가 잘못되면 컨테이너가 뜨지 않고 `docker logs` 에 `jwt.private-key 가 …` 로 시작하는 원인이 남습니다. 정상 기동했다면 JWKS 로 확인합니다.

JWKS 는 **외부에서 보이지 않는 것이 정상**입니다. 청첩장 auth-api 는 API 게이트웨이 뒤에 있고 게이트웨이에 이 경로 라우트가 없으며, 청첩장 경로에서는 JWKS 를 읽는 쪽이 없습니다(검증은 auth-api 가 introspect 안에서 합니다). lnb-api 는 같은 docker 네트워크에서 `http://auth-api:9000/.well-known/jwks.json` 을 직접 읽습니다. 그래서 확인은 서버 안에서 합니다.

```bash
# EC2 에 ssh 로 들어간 뒤
docker exec <auth-api 컨테이너명> wget -qO- http://localhost:9000/.well-known/jwks.json
```

`keys[0].kid` 가 비어 있지 않고 `n` 이 있으면 정상입니다. 컨테이너에 wget 이 없으면 같은 네트워크의 다른 컨테이너에서 `curl http://auth-api:9000/.well-known/jwks.json` 으로 봅니다.

#### HS256 → RS256 전환 절차

`JWT_PRIVATE_KEY` 를 **추가**하고 `JWT_SECRET` 은 **유지**한 채 배포한다(세션 유지). `JWT_REFRESH_TOKEN_EXPIRATION` 이상 지난 뒤 `JWT_SECRET` 을 지우고 재배포한다.

전환 시 유의:

- `JWT_SECRET` 을 너무 일찍 지우면: 옛 HS256 AT 를 가진 브라우저는 introspect 에서 쿠키가 지워지지 않은 채 401 을 받고, AT 쿠키 만료(`JWT_ACCESS_TOKEN_EXPIRATION`) 뒤 silent refresh 경로에서 쿠키가 지워져 재로그인하게 됩니다. `JWT_REFRESH_TOKEN_EXPIRATION` 이상 기다리면 이 상황이 생기지 않습니다.
- 롤백: RS256 이미지에서 HS256 이미지로 되돌리면 그 사이 발급된 세션은 전부 깨집니다. 롤백 가능성을 남기려면 전환 창 동안 `JWT_SECRET` 을 유지합니다.
- 키 교체(`JWT_PRIVATE_KEY` 변경)는 `kid` 가 바뀌어 기존 세션 전원 로그아웃이며, JWKS 를 캐시하는 소비자(lnb-api)도 재시작해야 합니다. 회전 지원은 이번 범위 밖입니다.
- 토큰 크기: RS256 서명은 HS256 보다 약 300 바이트 깁니다(AT·RT 각각). 쿠키·`X-New-AT`/`X-New-RT` 헤더 크기가 그만큼 늘어납니다.
