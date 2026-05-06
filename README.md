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
  - **배포 주소**: http://54.180.105.89/

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

```
src/main/java/com/springboot/monew                                              
  ├── MoNewApplication.java                                                                       
  ├── comment                                                                                     
  │   ├── controller                                                                              
  │   │   ├── CommentApiDocs.java                                                                 
  │   │   ├── CommentController.java                                                              
  │   │   ├── CommentLikeApiDocs.java                                                             
  │   │   └── CommentLikeController.java                                                          
  │   ├── dto                                                                                     
  │   │   ├── CommentDto.java                                                                     
  │   │   ├── CommentLikeDto.java                                                                 
  │   │   ├── CommentPageRequest.java                                                             
  │   │   ├── CommentRegisterRequest.java                                                         
  │   │   ├── CommentUpdateRequest.java                                                           
  │   │   └── CursorPageResponseCommentDto.java                                                   
  │   ├── entity                                                                                  
  │   │   ├── Comment.java                                                                     
  │   │   ├── CommentDirection.java                                                               
  │   │   ├── CommentLike.java                                                                 
  │   │   └── CommentOrderBy.java                                                                 
  │   ├── exception
  │   │   ├── CommentErrorCode.java                                                               
  │   │   └── CommentException.java                                                            
  │   ├── mapper                                                                                  
  │   │   ├── CommentLikeMapper.java
  │   │   └── CommentMapper.java                                                                  
  │   ├── repository                                                                              
  │   │   ├── CommentLikeRepository.java                                                       
  │   │   ├── CommentRepository.java                                                              
  │   │   └── qdsl                                                                             
  │   │       ├── CommentQDSLRepository.java                                                      
  │   │       └── CommentQDSLRepositoryImpl.java
  │   └── service                                                                                 
  │       ├── CommentLikeService.java                                                          
  │       └── CommentService.java                                                                 
  ├── common
  │   ├── dto                                                                                     
  │   │   └── CursorPageResponse.java                                                          
  │   ├── entity                                                                                  
  │   │   ├── BaseEntity.java
  │   │   └── BaseUpdatableEntity.java                                                            
  │   ├── exception                                                                               
  │   │   ├── ErrorCode.java                                                                   
  │   │   ├── ErrorResponse.java                                                                  
  │   │   ├── GlobalExceptionHandler.java                                                      
  │   │   └── MonewException.java                                                                 
  │   ├── log
  │   │   ├── LogFileItemReader.java                                                              
  │   │   ├── LogUploadBatchConfig.java                                                           
  │   │   ├── LogUploadBatchService.java                                                       
  │   │   ├── LogUploadScheduler.java                                                             
  │   │   └── S3UploadItemWriter.java                                                          
  │   ├── mapper                                                                                  
  │   │   ├── BaseMapper.java                                                                     
  │   │   └── CommonMapperConfig.java                                                          
  │   ├── metric                                                                                  
  │   │   ├── MetricSupport.java                                                               
  │   │   ├── MonewMetricTags.java                                                                
  │   │   ├── MonewTaskNames.java                                                                 
  │   │   ├── ScheduledTaskMetrics.java                                                        
  │   │   ├── ScheduledTaskStatus.java                                                            
  │   │   └── TaskMetricNames.java                                                             
  │   └── utils                                                                                   
  │       └── TimeConverter.java                                                               
  ├── config                                                                                      
  │   ├── BatchConfig.java                                                                     
  │   ├── HttpClientConfig.java                                                                
  │   ├── JpaAuditingConfig.java                                                                  
  │   ├── JpaTransactionConfig.java                                                               
  │   ├── MdcLoggingInterceptor.java                                                              
  │   ├── MongoConfig.java                                                                        
  │   ├── NotificationCreationConfig.java                                                      
  │   ├── QueryDslConfig.java                                                                     
  │   ├── S3Config.java                                                                           
  │   ├── SchedulingConfig.java                                                                
  │   ├── SwaggerConfig.java                                                                      
  │   └── WebMvcConfig.java                                                                       
  ├── interest                                                                                 
  │   ├── controller                                                                              
  │   │   ├── InterestApiDocs.java                                                             
  │   │   └── InterestController.java                                                             
  │   ├── dto
  │   │   ├── request                                                                             
  │   │   │   ├── InterestPageRequest.java                                                     
  │   │   │   ├── InterestRegisterRequest.java                                                    
  │   │   │   └── InterestUpdateRequest.java
  │   │   └── response                                                                            
  │   │       ├── CursorPageResponseInterestDto.java                                           
  │   │       ├── InterestDto.java                                                                
  │   │       ├── InterestKeywordInfo.java
  │   │       └── SubscriptionDto.java                                                            
  │   ├── entity                                                                               
  │   │   ├── Interest.java                                                                       
  │   │   ├── InterestDirection.java                                                              
  │   │   ├── InterestKeyword.java                                                             
  │   │   ├── InterestOrderBy.java                                                                
  │   │   ├── Keyword.java                                                                     
  │   │   └── Subscription.java                                                                   
  │   ├── exception
  │   │   ├── InterestErrorCode.java                                                              
  │   │   └── InterestException.java                                                              
  │   ├── mapper                                                                               
  │   │   ├── InterestDtoMapper.java                                                              
  │   │   └── SubscriptionDtoMapper.java                                                       
  │   ├── repository                                                                              
  │   │   ├── InterestKeywordRepository.java
  │   │   ├── InterestRepository.java                                                             
  │   │   ├── KeywordRepository.java                                                           
  │   │   ├── SubscriptionRepository.java                                                         
  │   │   └── qdsl
  │   │       ├── InterestQDSLRepository.java                                                     
  │   │       └── InterestQDSLRepositoryImpl.java                                              
  │   ├── service                                                                                 
  │   │   ├── InterestService.java
  │   │   └── SubscriptionService.java                                                            
  │   └── util                                                                                    
  │       └── StringSimilarityUtil.java                                                        
  ├── newsarticles                                                                                
  │   ├── controller                                                                              
  │   │   ├── NewsArticleApiDocs.java                                                          
  │   │   └── NewsArticleController.java                                                          
  │   ├── dto                                                                                  
  │   │   ├── CollectedArticleWithInterest.java                                                   
  │   │   ├── NaverNewsItem.java
  │   │   ├── NewsArticleBackupDto.java                                                           
  │   │   ├── ParsedCursor.java                                                                
  │   │   ├── RssItem.java                                                                        
  │   │   ├── request                                                                          
  │   │   │   └── NewsArticlePageRequest.java                                                     
  │   │   └── response                                                                         
  │   │       ├── CollectedArticle.java                                                           
  │   │       ├── CursorPageResponseNewsArticleDto.java                                           
  │   │       ├── NaverNewsResponse.java                                                       
  │   │       ├── NewsArticleCursorRow.java                                                       
  │   │       ├── NewsArticleDto.java                                                             
  │   │       ├── NewsArticleViewDto.java                                                      
  │   │       └── RestoreResultDto.java                                                           
  │   ├── entity                                                                               
  │   │   ├── ArticleInterest.java                                                                
  │   │   ├── ArticleView.java                                                                    
  │   │   └── NewsArticle.java                                                                 
  │   ├── enums                                                                                   
  │   │   ├── ArticleSource.java                                                               
  │   │   ├── NewsArticleDirection.java                                                           
  │   │   └── NewsArticleOrderBy.java                                                             
  │   ├── exception                                                                            
  │   │   ├── ArticleException.java                                                               
  │   │   └── NewsArticleErrorCode.java                                                        
  │   ├── mapper                                                                                  
  │   │   ├── NewsArticleMapper.java
  │   │   └── NewsArticleViewMapper.java                                                          
  │   ├── metric                                                                                  
  │   │   ├── NewsBackupMetrics.java                                                           
  │   │   ├── NewsCollectMetrics.java                                                             
  │   │   ├── NewsMetricNames.java                                                                
  │   │   └── result                                                                           
  │   │       ├── NewsArticleCollectResult.java                                                   
  │   │       ├── NewsArticleSaveResult.java                                                      
  │   │       ├── NewsArticleSourceCollectResult.java                                          
  │   │       ├── NewsBackupFileResult.java                                                       
  │   │       └── NewsBackupRunResult.java                                                     
  │   ├── repository                                                                              
  │   │   ├── ArticleInterestRepository.java                                                   
  │   │   ├── ArticleViewRepository.java                                                          
  │   │   ├── NewsArticleRepository.java                                                       
  │   │   └── qdsl                                                                                
  │   │       ├── NewsArticleQDSLRepository.java
  │   │       └── NewsArticleQDSLRepositoryImpl.java                                              
  │   ├── s3                                                                                      
  │   │   ├── AwsProperties.java                                                               
  │   │   ├── NewsArticleBackupService.java                                                       
  │   │   ├── NewsArticleRestoreService.java                                                      
  │   │   └── S3BackupService.java                                                             
  │   ├── scheduler                                                                               
  │   │   ├── NewsArticleBackupScheduler.java                                                  
  │   │   └── NewsArticleScheduler.java                                                           
  │   └── service                                                                                 
  │       ├── NaverNewsApiClient.java                                                          
  │       ├── NewsArticleCollectService.java                                                      
  │       ├── NewsArticleService.java                                                             
  │       ├── RssClient.java                                                                   
  │       └── collector                                                                           
  │           ├── ArticleCollector.java                                                        
  │           ├── ChosunRssCollector.java                                                         
  │           ├── HankyungRssCollector.java
  │           ├── NaverArticleCollector.java                                                      
  │           └── YonhapRssCollector.java                                                      
  ├── notification                                                                                
  │   ├── controller                                                                           
  │   │   ├── NotificationApiDocs.java                                                            
  │   │   └── NotificationController.java                                                      
  │   ├── dto                                                                                     
  │   │   ├── NotificationDto.java                                                             
  │   │   └── NotificationFindRequest.java
  │   ├── entity                                                                                  
  │   │   ├── Notification.java
  │   │   └── ResourceType.java                                                                   
  │   ├── event                                                                                   
  │   │   ├── CommentLikeNotificationEvent.java
  │   │   ├── InterestNotificationEvent.java                                                      
  │   │   └── listener                                                                            
  │   │       └── NotificationEventListener.java
  │   ├── exception                                                                               
  │   │   ├── NotificationErrorCode.java                                                       
  │   │   └── NotificationException.java                                                          
  │   ├── mapper
  │   │   └── NotificationMapper.java                                                             
  │   ├── metric                                                                               
  │   │   ├── NotificationMetricNames.java
  │   │   └── NotificationMetrics.java                                                            
  │   ├── repository
  │   │   ├── NotificationRepository.java                                                         
  │   │   └── qdsl                                                                             
  │   │       ├── NotificationQDSLRepository.java                                                 
  │   │       └── NotificationQDSLRepositoryImpl.java
  │   ├── scheduler                                                                               
  │   │   └── NotificationCleanUpScheduler.java                                                   
  │   └── service
  │       └── NotificationService.java                                                            
  └── user                                                                                     
      ├── controller                                                                              
      │   ├── UserActivityController.java
      │   ├── UserActivityDocs.java                                                               
      │   ├── UserApiDocs.java                                                                 
      │   └── UserController.java                                                                 
      ├── document
      │   └── UserActivityDocument.java                                                           
      ├── dto                                                                                  
      │   ├── request
      │   │   ├── UserLoginRequest.java
      │   │   ├── UserRegisterRequest.java                                                        
      │   │   └── UserUpdateRequest.java
      │   └── response                                                                            
      │       ├── CommentActivityDto.java                                                      
      │       ├── CommentLikeActivityDto.java                                                     
      │       ├── UserActivityDto.java
      │       └── UserDto.java                                                                    
      ├── entity                                                                               
      │   └── User.java                                                                           
      ├── event
      │   ├── UserActivityEventListener.java                                                      
      │   ├── articleView                                                                         
      │   │   └── ArticleViewedEvent.java
      │   ├── comment                                                                             
      │   │   ├── CommentCreatedEvent.java                                                     
      │   │   ├── CommentDeletedEvent.java                                                        
      │   │   ├── CommentLikeCountUpdatedEvent.java
      │   │   ├── CommentLikedEvent.java                                                          
      │   │   ├── CommentUnlikedEvent.java                                                        
      │   │   └── CommentUpdatedEvent.java
      │   ├── interest                                                                            
      │   │   ├── InterestSubscribedEvent.java                                                 
      │   │   ├── InterestUnsubscribedEvent.java                                                  
      │   │   └── InterestUpdatedEvent.java
      │   └── user                                                                                
      │       ├── UserNicknameUpdatedEvent.java                                                   
      │       └── UserRegisteredEvent.java
      ├── exception                                                                               
      │   ├── UserErrorCode.java                                                               
      │   └── UserException.java
      ├── mapper                                                                                  
      │   ├── UserActivityMapper.java
      │   └── UserMapper.java                                                                     
      ├── metric                                                                               
      │   ├── UserMetricNames.java                                                                
      │   └── UserMetrics.java
      ├── outbox                                                                                  
      │   ├── UserActivityOutbox.java                                                          
      │   ├── UserActivityOutboxPayloadSerializer.java
      │   ├── UserActivityOutboxProcessor.java                                                    
      │   ├── UserActivityOutboxSingleProcessor.java
      │   ├── enums                                                                               
      │   │   ├── UserActivityAggregateType.java                                                  
      │   │   ├── UserActivityEventType.java
      │   │   └── UserActivityOutboxStatus.java                                                   
      │   └── payload                                                                          
      │       ├── articleview                                                                     
      │       │   └── ArticleViewedPayload.java
      │       ├── comment                                                                         
      │       │   ├── CommentActivityPayload.java                                              
      │       │   └── CommentDeletedPayload.java                                                  
      │       ├── commentlike
      │       │   ├── CommentLikeActivityPayload.java                                             
      │       │   ├── CommentLikeCountUpdatedPayload.java                                         
      │       │   └── CommentUnlikedPayload.java
      │       ├── interest                                                                        
      │       │   ├── InterestSubscribedPayload.java                                              
      │       │   ├── InterestUnsubscribedPayload.java
      │       │   └── InterestUpdatedPayload.java                                                 
      │       └── user                                                                         
      │           ├── UserNicknameUpdatedPayload.java                                             
      │           └── UserRegisteredPayload.java
      ├── repository                                                                              
      │   ├── UserActivityOutboxRepository.java                                                
      │   ├── UserActivityRepository.java                                                         
      │   ├── UserRepository.java
      │   └── qdsl                                                                                
      │       ├── UserQDSLRepository.java                                                      
      │       └── UserQDSLRepositoryImpl.java                                                     
      ├── scheduler
      │   ├── UserActivityOutboxScheduler.java                                                    
      │   └── UserCleanUpScheduler.java                                                        
      └── service                                                                                 
          ├── UserActivityOutboxService.java
          ├── UserActivityService.java                                                            
          ├── UserActivityUpdateService.java                                                   
          └── UserService.java
```

  <br>

  ## 📝 프로젝트 회고록

  > 제작한 발표자료 링크 또는 첨부파일 첨부
