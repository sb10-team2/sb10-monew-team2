# MoNew
  ### 뉴스 큐레이션 및 소셜 플랫폼

  > 여러 뉴스 API를 통합하여 사용자에게 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을
  갖춘 서비스

  <br>

  ## 👥 팀원 구성

  | 전승주 (팀장) | 곽인성 | 이윤섭 | 성주현 | 전창현 |

  <br>

  ## 📖 프로젝트 소개

  MongoDB 및 PostgreSQL 백업 및 복구 시스템

  - **프로젝트 기간**: 2026.04.14 ~ 2026.05.08

  <br>

  ## 🛠 기술 스택

  - **Backend**: Spring Boot 3.5, Spring Data JPA, Spring Batch, QueryDSL
  - **Database**: PostgreSQL 16, MongoDB Atlas
  - **Infra**: AWS ECS(EC2), RDS, ECR, S3, GitHub Actions, Docker
  - **Monitoring**: Spring Actuator, Prometheus
  - **Docs**: Swagger (SpringDoc OpenAPI)
  - **Test**: JUnit5, Testcontainers, Instancio, JaCoCo
  - **공통 Tool**: Git & Github, Discord, Notion

  <br>

  ## ⚙️ 팀원별 구현 기능

  ### 전승주 (팀장)

  **댓글 도메인 (Comment / CommentLike)**
  - 댓글 등록 / 수정 / 논리 삭제 / 물리 삭제 API 구현
  - 댓글 좋아요 / 좋아요 취소 API 구현
  - 커서 기반 페이지네이션 적용 (createdAt / likeCount 정렬)
  - 댓글 목록 조회 성능 개선 (불필요한 COUNT 쿼리 제거, 복합 인덱스 추가)
  - S3 로그 적재 스프링 배치 적용
  - 댓글 도메인 통합 테스트 (Testcontainers)
  - CI 파이프라인 구축

  ---

  ### 곽인성

  **뉴스기사 도메인 (NewsArticle)**
  - Naver Search API 및 RSS(한경/조선/연합) 뉴스 수집 배치 구현
  - 뉴스기사 S3 백업 / 복구 배치 구현
  - 뉴스기사 조회 API 및 QueryDSL 기반 커서 페이지네이션 구현
  - 알림 이벤트 발행 구조 개선 (단건 발행 전환)
  - CD 파이프라인 구축 (GitHub Actions → ECR → ECS)
  - 뉴스기사 도메인 슬라이스 테스트 / 통합 테스트

  ---

  ### 이윤섭
  **알림 도메인 (Notification)**
  - 부하 테스트 및 테스트 데이터 생성
  - K6 기반 부하 테스트 설계 및 수행
  - 테스트용 대용량 데이터 생성기 구현 (User, NewsArticle, Interest, ArticleView 등)
  - 관심사 QueryDSL 테스트 코드 작성

  ---

  ### 성주현

  **모니터링 및 관심사 도메인**
  - 관심사 등록/수정/구독/삭제 기능 구현
  - Spring Actuator + Prometheus 메트릭 수집 구현
  - 스케줄러(뉴스 수집 / 알림 삭제 / 사용자 삭제) 메트릭 적용
  - 관심사 통합 테스트 (Testcontainers)

  ---

  ### 전창현

  **사용자 도메인 (User / UserActivity)**
  - 사용자 등록 / 로그인 / 수정 / 논리 삭제 / 물리 삭제 API 구현
  - MongoDB 기반 사용자 활동 내역 관리 (구독 관심사, 댓글, 좋아요, 기사 조회)
  - Outbox 패턴 적용 (이벤트 기반 UserActivity 갱신, 재처리 및 멱등성 보강)
  - 사용자 도메인 통합 테스트 / 활동 내역 통합 테스트

  <br>

  ## 📂 파일 구조

  src/main/java/com/springboot/monew                                                              
  ├── comment                                                                                   
  │   ├── controller                                                                              
  │   ├── dto
  │   ├── entity                                                                                  
  │   ├── exception                                                                               
  │   ├── mapper
  │   ├── repository                                                                              
  │   │   └── qdsl
  │   └── service                                                                                 
  ├── common                                                                                      
  │   ├── dto                                                                                     
  │   ├── entity
  │   ├── exception                                                                               
  │   ├── log                                                                                     
  │   ├── mapper
  │   ├── metric
  │   └── utils                                                                                   
  ├── config
  ├── interest                                                                                    
  │   ├── controller                                                                              
  │   ├── dto
  │   │   ├── request
  │   │   └── response
  │   ├── entity                                                                                  
  │   ├── exception
  │   ├── mapper                                                                                  
  │   ├── repository                                                                              
  │   │   └── qdsl
  │   ├── service
  │   └── util                                                                                    
  ├── newsarticles
  │   ├── controller                                                                              
  │   ├── dto                                                                                     
  │   │   ├── request
  │   │   └── response
  │   ├── entity
  │   ├── enums                                                                                   
  │   ├── exception
  │   ├── mapper                                                                                  
  │   ├── metric                                                                                  
  │   │   └── result
  │   ├── repository                                                                              
  │   │   └── qdsl
  │   ├── s3                                                                                      
  │   ├── scheduler                                                                               
  │   └── service
  │       └── collector                                                                           
  ├── notification
  │   ├── controller                                                                              
  │   ├── dto                                                                                     
  │   ├── entity
  │   ├── event
  │   │   └── listener
  │   ├── exception                                                                               
  │   ├── mapper
  │   ├── metric                                                                                  
  │   ├── repository                                                                              
  │   │   └── qdsl
  │   ├── scheduler
  │   └── service                                                                                 
  └── user
      ├── controller                                                                              
      ├── document                                                                                
      ├── dto
      │   ├── request
      │   └── response
      ├── entity                                                                                  
      ├── event
      │   ├── articleView                                                                         
      │   ├── comment                                                                             
      │   ├── interest
      │   └── user                                                                                
      ├── exception
      ├── mapper                                                                                  
      ├── metric                                                                                  
      ├── outbox
      │   ├── enums
      │   └── payload                                                                             
      │       ├── articleview
      │       ├── comment                                                                         
      │       ├── commentlike                                                                     
      │       ├── interest                                                                        
      │       └── user
      ├── repository                                                                              
      │   └── qdsl                                                                                
      ├── scheduler
      └── service

  <br>

  ## 📝 프로젝트 회고록

  > 제작한 발표자료 링크 또는 첨부파일 첨부
