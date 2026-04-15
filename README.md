# 모뉴
### MongoDB 및 PostgreSQL 백업 및 복구 시스템

> 여러 뉴스 API를 통합하여 사용자에게 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을 갖춘 서비스

<br>

## 👥 팀원 구성

| 전승주 (팀장) | 곽인성 | 이윤섭 | 성주현 | 전창현 |
|---|---|---|---|---|
| [Github](개인 Github 링크) | [Github](개인 Github 링크) | [Github](개인 Github 링크) | [Github](개인 Github 링크) | [Github](개인 Github 링크) |

<br>

## 📖 프로젝트 소개

프로그래밍 교육 사이트의 Spring 백엔드 시스템 구축

- **프로젝트 기간**: 2026.04.14 ~ 2026.05.08

<br>

## 🛠 기술 스택

- **Backend**: Spring Boot, Spring Data JPA, Spring Batch
- **Database**: PostgresSQL, MongoDB
- **공통 Tool**: Git & Github, Discord, Notion

<br>

## ⚙️ 팀원별 구현 기능

### 웨인 (예시)

> 자신이 개발한 기능에 대한 사진이나 gif 파일 첨부

**소셜 로그인 API**
- Google OAuth 2.0을 활용한 소셜 로그인 기능 구현
- 로그인 후 추가 정보 입력을 위한 RESTful API 엔드포인트 개발

**회원 추가 정보 입력 API**
- 회원 유형(관리자, 학생)에 따른 조건부 입력 처리 API 구현

---



<br>

## 📂 파일 구조

```
src
 ┣ main
 ┃ ┣ java
 ┃ ┃ ┣ com
 ┃ ┃ ┃ ┣ example
 ┃ ┃ ┃ ┃ ┣ controller
 ┃ ┃ ┃ ┃ ┃ ┣ AuthController.java
 ┃ ┃ ┃ ┃ ┃ ┣ UserController.java
 ┃ ┃ ┃ ┃ ┃ ┗ AdminController.java
 ┃ ┃ ┃ ┃ ┣ model
 ┃ ┃ ┃ ┃ ┃ ┣ User.java
 ┃ ┃ ┃ ┃ ┃ ┗ Course.java
 ┃ ┃ ┃ ┃ ┣ repository
 ┃ ┃ ┃ ┃ ┃ ┣ UserRepository.java
 ┃ ┃ ┃ ┃ ┃ ┗ CourseRepository.java
 ┃ ┃ ┃ ┃ ┣ service
 ┃ ┃ ┃ ┃ ┃ ┣ AuthService.java
 ┃ ┃ ┃ ┃ ┃ ┣ UserService.java
 ┃ ┃ ┃ ┃ ┃ ┗ AdminService.java
 ┃ ┃ ┃ ┃ ┣ security
 ┃ ┃ ┃ ┃ ┃ ┣ SecurityConfig.java
 ┃ ┃ ┃ ┃ ┃ ┗ JwtAuthenticationEntryPoint.java
 ┃ ┃ ┃ ┃ ┣ dto
 ┃ ┃ ┃ ┃ ┃ ┣ LoginRequest.java
 ┃ ┃ ┃ ┃ ┃ ┗ UserResponse.java
 ┃ ┃ ┃ ┃ ┣ exception
 ┃ ┃ ┃ ┃ ┃ ┣ GlobalExceptionHandler.java
 ┃ ┃ ┃ ┃ ┃ ┗ ResourceNotFoundException.java
 ┃ ┃ ┃ ┃ ┣ utils
 ┃ ┃ ┃ ┃ ┃ ┣ JwtUtils.java
 ┃ ┃ ┃ ┃ ┃ ┗ UserMapper.java
 ┃ ┃ ┃ ┣ resources
 ┃ ┃ ┃ ┃ ┣ application.properties
 ┃ ┃ ┃ ┃ ┗ static
 ┃ ┃ ┃ ┃ ┃ ┣ css
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ style.css
 ┃ ┃ ┃ ┃ ┃ ┣ js
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ script.js
 ┃ ┃ ┃ ┣ webapp
 ┃ ┃ ┃ ┃ ┣ WEB-INF
 ┃ ┃ ┃ ┃ ┃ ┗ web.xml
 ┃ ┃ ┃ ┣ test
 ┃ ┃ ┃ ┃ ┣ java
 ┃ ┃ ┃ ┃ ┃ ┣ com
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ example
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ AuthServiceTest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ UserControllerTest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ ApplicationTests.java
 ┃ ┃ ┃ ┣ resources
 ┃ ┃ ┃ ┃ ┣ application.properties
 ┃ ┃ ┃ ┃ ┗ static
 ┃ ┃ ┃ ┃ ┃ ┣ css
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ style.css
 ┃ ┃ ┃ ┃ ┃ ┣ js
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ script.js
 ┣ pom.xml
 ┣ Application.java
 ┣ application.properties
 ┣ .gitignore
 ┗ README.md
```

<br>

## 🌐 구현 홈페이지

[https://www.codeit.kr/](https://www.codeit.kr/)

<br>

## 📝 프로젝트 회고록

> 제작한 발표자료 링크 혹은 첨부파일 첨부
