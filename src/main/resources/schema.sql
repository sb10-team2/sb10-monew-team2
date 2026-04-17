CREATE TABLE interest_keywords (
                                     "id"	UUID		PRIMARY KEY,
                                     "interest_id"	UUID		NOT NULL,
                                     "keyword_id"	UUID		NOT NULL,
                                     "created_at"	TIMESTAMPTZ		NOT NULL
);

-- 뉴스기사 조회 기록
CREATE TABLE article_views (
                                 "id"	UUID		PRIMARY KEY,
                                 "news_article_id"	UUID		NOT NULL,
                                 "user_id"	UUID		NOT NULL,
                                 "created_at"	TIMESTAMPTZ		NOT NULL
);

-- 뉴스기사 관심사 연결
CREATE TABLE article_interests (
                                     "id"	UUID		PRIMARY KEY,
                                     "news_article_id"	UUID		NOT NULL,
                                     "interest_id"	UUID		NOT NULL,
                                     "created_at"	TIMESTAMPTZ		NOT NULL
);

CREATE TABLE subscriptions (
                                 "id"	UUID		PRIMARY KEY,
                                 "user_id"	UUID		NOT NULL,
                                 "interest_id"	UUID		NOT NULL,
                                 "created_at"	TIMESTAMPTZ		NOT NULL
);

CREATE TABLE comment_likes (
                                 "id"	UUID		PRIMARY KEY,
                                 "created_at"	TIMESTAMPTZ		NOT NULL,
                                 "comment_id"	UUID		NOT NULL,
                                 "user_id"	UUID		NOT NULL
);

CREATE TABLE notifications (
                                 "id"	UUID		PRIMARY KEY,
                                 "created_at"	TIMESTAMPTZ		NOT NULL,
                                 "updated_at"	TIMESTAMPTZ		NOT NULL,
                                 "confirmed"	BOOLEAN	DEFAULT false	NOT NULL,
                                 "content"	VARCHAR(100)		NOT NULL,
                                 "resource_type"	VARCHAR(10)		NOT NULL,
                                 "user_id"	UUID		NOT NULL,
                                 "interest_id"	UUID,
                                 "comment_likes_id" UUID
);

CREATE TABLE comments (
                            "id"	UUID		PRIMARY KEY,
                            "user_id"	UUID		NOT NULL,
                            "article_id"	UUID		NOT NULL,
                            "content"	VARCHAR(200)		NOT NULL,
                            "like_count"	INTEGER	DEFAULT 0	NOT NULL,
                            "is_deleted"	BOOLEAN		DEFAULT false NOT NULL,
                            "created_at"	TIMESTAMPTZ		NOT NULL,
                            "updated_at"	TIMESTAMPTZ		NULL
);

CREATE TABLE interests (
                             "id"	UUID		PRIMARY KEY,
                             "name"	VARCHAR(50)		NOT NULL,
                             "subscriber_count"	INTEGER	DEFAULT 0	NOT NULL,
                             "created_at"	TIMESTAMPTZ		NOT NULL,
                             "updated_at"	TIMESTAMPTZ		NULL
);

CREATE TABLE keywords (
                            "id"	UUID		PRIMARY KEY,
                            "name"	VARCHAR(100)		NOT NULL,
                            "created_at"	TIMESTAMPTZ		NOT NULL
);

-- 사용자 테이블
CREATE TABLE users (
                         "id" UUID PRIMARY KEY,
                         "email"	VARCHAR(100)		NOT NULL,
                         "nickname"	VARCHAR(20)		NOT NULL,
                         "password"	VARCHAR(255)		NOT NULL,
                         "deleted_at"	TIMESTAMPTZ		NULL,
                         "created_at"	TIMESTAMPTZ		NOT NULL,
                         "updated_at"	TIMESTAMPTZ		NULL
);

-- 뉴스기사 테이블
CREATE TABLE news_articles (
                                 "id"	UUID	PRIMARY KEY,
                                 "source"	VARCHAR(100)	NOT NULL,
                                 "original_link"	VARCHAR(500)	NOT NULL,
                                 "title"	VARCHAR(300)	NOT NULL,
                                 "published_at"	TIMESTAMPTZ  NOT NULL,
                                 "summary"	TEXT,
                                 "view_count"	INTEGER	NOT NULL DEFAULT 0,
                                 "is_deleted"	BOOLEAN	NOT	NULL
);

-- =========================
-- news_articles 제약조건
-- =========================
ALTER TABLE news_articles
    ADD CONSTRAINT "UK_NEWS_ARTICLES_ORIGINAL_LINK" UNIQUE ("original_link"),
ADD CONSTRAINT "CK_NEWS_ARTICLES_VIEW_COUNT" CHECK ("view_count" >= 0);

-- =========================
-- article_interests 제약조건
-- =========================
ALTER TABLE article_interests
    ADD CONSTRAINT "UK_ARTICLE_INTERESTS_ARTICLE_INTEREST" UNIQUE ("news_article_id", "interest_id"),
ADD CONSTRAINT "FK_ARTICLE_INTERESTS_NEWS_ARTICLE"
    FOREIGN KEY ("news_article_id")
    REFERENCES "news_articles" ("id")
    ON DELETE CASCADE,
ADD CONSTRAINT "FK_ARTICLE_INTERESTS_INTEREST"
    FOREIGN KEY ("interest_id")
    REFERENCES "interests" ("id")
    ON DELETE CASCADE;

-- =========================
-- article_views 제약조건
-- =========================
ALTER TABLE article_views
    ADD CONSTRAINT "UK_ARTICLE_VIEWS_ARTICLE_USER" UNIQUE ("news_article_id", "user_id"),
ADD CONSTRAINT "FK_ARTICLE_VIEWS_NEWS_ARTICLE"
    FOREIGN KEY ("news_article_id")
    REFERENCES "news_articles" ("id")
    ON DELETE CASCADE,
ADD CONSTRAINT "FK_ARTICLE_VIEWS_USER"
    FOREIGN KEY ("user_id")
    REFERENCES "users" ("id")
    ON DELETE CASCADE;

-- =========================
-- comments 제약조건
-- =========================
ALTER TABLE comments
    ADD CONSTRAINT "FK_COMMENTS_NEWS_ARTICLE"
        FOREIGN KEY ("article_id")
            REFERENCES "news_articles" ("id")
            ON DELETE CASCADE;

-- 이메일 중복 방지
ALTER TABLE users
    ADD CONSTRAINT "UK_USERS_EMAIL" UNIQUE ("email");

-- 닉네임 중복 방지
ALTER TABLE users
    ADD CONSTRAINT "UK_USERS_NICKNAME" UNIQUE ("nickname");

-- "comments" 테이블에서 "users"의 ID를 참조한다.
-- 유저 삭제 시 해당 유저의 댓글도 함께 삭제
ALTER TABLE comments
    ADD CONSTRAINT "FK_COMMENTS_USER_ID"
        FOREIGN KEY ("user_id")
            REFERENCES "users" ("id")
            ON DELETE CASCADE;

-- "comment_likes" 테이블에서 "users"의 ID를 참조한다.
-- 유저 삭제 시 해당 유저의 좋아요 기록도 삭제
ALTER TABLE comment_likes
    ADD CONSTRAINT "FK_COMMENT_LIKES_USER_ID"
        FOREIGN KEY ("user_id")
            REFERENCES "users" ("id")
            ON DELETE CASCADE;

-- comment_likes 중복 방지
ALTER TABLE comment_likes
    ADD CONSTRAINT "UQ_COMMENT_LIKES"
        UNIQUE ("comment_id", "user_id");

-- comments 삭제 시 좋아요도 함께 삭제
ALTER TABLE comment_likes
    ADD CONSTRAINT "FK_COMMENT_LIKES_COMMENT_ID"
        FOREIGN KEY ("comment_id")
            REFERENCES "comments" ("id")
            ON DELETE CASCADE;

-- =========================
-- notifications 제약조건
-- =========================

ALTER TABLE notifications
    ADD CONSTRAINT "FK_NOTIFICATIONS_USER_ID" FOREIGN KEY ("user_id") REFERENCES "users" ("id") on delete cascade,
    ADD CONSTRAINT "FK_NOTIFICATIONS_INTEREST_ID" FOREIGN KEY ("interest_id") REFERENCES "interests" ("id") on delete cascade,
    ADD CONSTRAINT "FK_NOTIFICATIONS_COMMENT_LIKES_ID" FOREIGN KEY ("comment_likes_id") REFERENCES "comment_likes" ("id") on delete cascade;

-- =========================
-- subscriptions 제약조건
-- =========================
-- 구독 테이블에서 사용자의 ID를 참조한다.
ALTER TABLE subscriptions ADD CONSTRAINT "FK_SUBSCRIPTIONS_USER_ID"
    FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE;

-- 구독 테이블에서 관심사의 ID를 참조한다.
ALTER TABLE subscriptions ADD CONSTRAINT "FK_SUBSCRIPTIONS_INTEREST_ID"
    FOREIGN KEY ("interest_id") REFERENCES "interests" ("id") ON DELETE CASCADE;

-- 하나의 사용자가 동일한 관심사를 중복 구독할 수 없다.
ALTER TABLE subscriptions ADD CONSTRAINT "UK_SUBSCRIPTIONS_USER_ID_INTEREST_ID"
    UNIQUE ("user_id", "interest_id");

-- =========================
-- interests 제약조건
-- =========================
-- 관심사 이름 중복 방지
ALTER TABLE interests ADD CONSTRAINT "UK_INTERESTS_NAME" UNIQUE ("name");

-- 관심사의 구독자 수는 0 이상이어야 한다.
ALTER TABLE interests ADD CONSTRAINT "CK_INTERESTS_SUBSCRIBER_COUNT"
    CHECK ("subscriber_count" >= 0);

-- =========================
-- keywords 제약조건
-- =========================
-- 키워드 이름 중복 방지
ALTER TABLE keywords ADD CONSTRAINT "UK_KEYWORDS_NAME" UNIQUE ("name");

-- =========================
-- interest_keywords 제약조건
-- =========================
-- 관심사-키워드 연결 테이블에서 관심사의 ID를 참조한다.
ALTER TABLE interest_keywords ADD CONSTRAINT "FK_INTEREST_KEYWORDS_INTEREST_ID"
    FOREIGN KEY ("interest_id") REFERENCES "interests" ("id") ON DELETE CASCADE;

-- 관심사-키워드 연결 테이블에서 키워드의 ID를 참조한다.
ALTER TABLE interest_keywords ADD CONSTRAINT "FK_INTEREST_KEYWORDS_KEYWORD_ID"
    FOREIGN KEY ("keyword_id") REFERENCES "keywords" ("id") ON DELETE CASCADE;

-- 하나의 관심사에 동일한 키워드가 중복될 수 없다.
ALTER TABLE interest_keywords ADD CONSTRAINT "UK_INTEREST_KEYWORDS_INTEREST_ID_KEYWORD_ID"
    UNIQUE ("interest_id", "keyword_id");

--- ==================
--- comments 테이블 제약 조건 : like_count는 0 미만으로 내려갈 수 없다!
--- ==================
ALTER TABLE comments ADD CONSTRAINT "CK_COMMENTS_LIKE_COUNT" CHECK ("like_count" >= 0);