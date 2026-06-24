# 🗂️ 공모집 백엔드

> 공모전·대외활동 정보를 한곳에서 모아볼 수 있는 공모집 서비스의 백엔드 레포지토리입니다.

<br>

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Database | MySQL |
| ORM | Spring Data JPA |
| API 문서 | springdoc-openapi (Swagger UI) |
| 코드 포매터 | Spotless (Palantir Java Format) |
| 빌드 도구 | Gradle |

<br>

## ⚙️ 로컬 실행 방법

### 1. 레포지토리 클론

```bash
git clone https://github.com/IT-Cotato/13th-Gongmozip-BE.git
cd 13th-Gongmozip-BE
```

### 2. 환경변수 설정

`.env.example`을 참고해 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=gongmozip
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

IntelliJ를 사용하는 경우 **Run/Debug Configurations → Environment variables**에 위 값을 입력합니다.

### 3. 실행

```bash
./gradlew bootRun
```

### 4. API 문서 확인

```
http://localhost:8080/swagger-ui/index.html
```


<br>

## ✍️ 커밋 컨벤션

| 이모지 | 타입 | 설명 |
|--------|------|------|
| ✨ `:sparkles:` | `feat` | 새로운 기능 추가 |
| 🐛 `:bug:` | `fix` | 버그 수정 |
| ♻️ `:recycle:` | `refactor` | 리팩토링 |
| 🎨 `:art:` | `style` | 코드 포맷팅 |
| 🔧 `:wrench:` | `chore` | 빌드·설정 변경 |
| 📝 `:memo:` | `docs` | 문서 수정 |
| 🔒 `:lock:` | `chore` | 보안 관련 |
| 🚧 `:construction:` | `wip` | 진행 중인 작업 |

```
:sparkles: feat: 게시글 목록 조회 API 구현
```

<br>

## 📐 코드 컨벤션

Spotless (Palantir Java Format)를 사용합니다. 빌드 시 자동으로 포맷이 적용됩니다.

수동으로 적용하려면 아래 명령어를 실행합니다.

```bash
./gradlew spotlessApply
```
