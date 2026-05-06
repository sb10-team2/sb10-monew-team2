CREATE TABLE interest_keywords
(
    id          UUID PRIMARY KEY,
    interest_id UUID                     NOT NULL,
    keyword_id  UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 뉴스기사 조회 기록
CREATE TABLE article_views
(
    id              UUID PRIMARY KEY,
    news_article_id UUID                     NOT NULL,
    user_id         UUID                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 뉴스기사 관심사 연결
CREATE TABLE article_interests
(
    id              UUID PRIMARY KEY,
    news_article_id UUID                     NOT NULL,
    interest_id     UUID                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    interest_id UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    comment_id UUID                     NOT NULL,
    user_id    UUID                     NOT NULL
);

CREATE TABLE notifications
(
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE,
    confirmed        BOOLEAN DEFAULT false    NOT NULL,
    content          VARCHAR(100)             NOT NULL,
    resource_type    VARCHAR(10)              NOT NULL,
    user_id          UUID                     NOT NULL,
    interest_id      UUID,
    comment_likes_id UUID
);

CREATE TABLE comments
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    article_id UUID                     NOT NULL,
    content    VARCHAR(200)             NOT NULL,
    like_count BIGINT DEFAULT 0        NOT NULL,
    is_deleted BOOLEAN DEFAULT false    NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(50)              NOT NULL,
    subscriber_count BIGINT DEFAULT 0         NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE keywords
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 사용자 테이블
CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(100)             NOT NULL,
    nickname   VARCHAR(20)              NOT NULL,
    password   VARCHAR(255)             NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL
);

-- 뉴스기사 테이블
CREATE TABLE news_articles
(
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    source        VARCHAR(100)             NOT NULL,
    original_link VARCHAR(500)             NOT NULL,
    title         VARCHAR(300)             NOT NULL,
    published_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    summary       TEXT                     NOT NULL,
    view_count    BIGINT DEFAULT 0         NOT NULL,
    is_deleted    BOOLEAN                  NOT NULL
);

-- 사용자 활동 이벤트 아웃박스 테이블
CREATE TABLE user_activity_outbox
(
    id             UUID PRIMARY KEY,
    event_type     VARCHAR(50)           NOT NULL,
    aggregate_type VARCHAR(30)           NOT NULL,
    aggregate_id   UUID                  NOT NULL,
    payload        TEXT                  NOT NULL,
    status         VARCHAR(20)           NOT NULL,
    retry_count    INTEGER DEFAULT 0     NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE          NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE           NULL,
    last_error     TEXT                  NULL,
    created_at     TIMESTAMP WITH TIME ZONE           NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE           NULL
);

-- =========================
-- news_articles 제약조건
-- =========================
ALTER TABLE news_articles
    ADD CONSTRAINT UK_NEWS_ARTICLES_ORIGINAL_LINK UNIQUE (original_link);
ALTER TABLE news_articles
    ADD CONSTRAINT CK_NEWS_ARTICLES_VIEW_COUNT CHECK (view_count >= 0);

-- =========================
-- article_interests 제약조건
-- =========================
ALTER TABLE article_interests
    ADD CONSTRAINT UK_ARTICLE_INTERESTS_ARTICLE_INTEREST UNIQUE (news_article_id, interest_id);
ALTER TABLE article_interests
    ADD CONSTRAINT FK_ARTICLE_INTERESTS_NEWS_ARTICLE FOREIGN KEY (news_article_id) REFERENCES news_articles (id) ON DELETE CASCADE;
ALTER TABLE article_interests
    ADD CONSTRAINT FK_ARTICLE_INTERESTS_INTEREST FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE;

-- =========================
-- article_views 제약조건
-- =========================
ALTER TABLE article_views
    ADD CONSTRAINT UK_ARTICLE_VIEWS_ARTICLE_USER UNIQUE (news_article_id, user_id);
ALTER TABLE article_views
    ADD CONSTRAINT FK_ARTICLE_VIEWS_NEWS_ARTICLE FOREIGN KEY (news_article_id) REFERENCES news_articles (id) ON DELETE CASCADE;
ALTER TABLE article_views
    ADD CONSTRAINT FK_ARTICLE_VIEWS_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- =========================
-- comments 제약조건
-- =========================
ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENTS_NEWS_ARTICLE FOREIGN KEY (article_id) REFERENCES news_articles (id) ON DELETE CASCADE;
ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENTS_USER_ID FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE comments
    ADD CONSTRAINT CK_COMMENTS_LIKE_COUNT CHECK (like_count >= 0);

-- =========================
-- 인덱스
-- =========================

CREATE INDEX idx_comments_article_deleted_created
    ON comments (article_id, is_deleted, created_at DESC);

CREATE INDEX idx_comments_article_deleted_likes_created
    ON comments (article_id, is_deleted, like_count DESC, created_at ASC);

-- =========================
-- users 제약조건
-- =========================
ALTER TABLE users
    ADD CONSTRAINT UK_USERS_EMAIL UNIQUE (email);
ALTER TABLE users
    ADD CONSTRAINT UK_USERS_NICKNAME UNIQUE (nickname);

-- =========================
-- comment_likes 제약조건
-- =========================
ALTER TABLE comment_likes
    ADD CONSTRAINT FK_COMMENT_LIKES_USER_ID FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE comment_likes
    ADD CONSTRAINT UQ_COMMENT_LIKES UNIQUE (comment_id, user_id);
ALTER TABLE comment_likes
    ADD CONSTRAINT FK_COMMENT_LIKES_COMMENT_ID FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE;

-- =========================
-- notifications 제약조건
-- =========================
ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_USER_ID FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_INTEREST_ID FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE;
ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_COMMENT_LIKES_ID FOREIGN KEY (comment_likes_id) REFERENCES comment_likes (id) ON DELETE CASCADE;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notification_polymorphic_match
        CHECK (
            (resource_type = 'COMMENT' AND comment_likes_id IS NOT NULL AND interest_id IS NULL)
                OR
            (resource_type = 'INTEREST' AND interest_id IS NOT NULL AND comment_likes_id IS NULL)
            );
-- =========================
-- subscriptions 제약조건
-- =========================
ALTER TABLE subscriptions
    ADD CONSTRAINT FK_SUBSCRIPTIONS_USER_ID FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE subscriptions
    ADD CONSTRAINT FK_SUBSCRIPTIONS_INTEREST_ID FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE;
ALTER TABLE subscriptions
    ADD CONSTRAINT UK_SUBSCRIPTIONS_USER_ID_INTEREST_ID UNIQUE (user_id, interest_id);

-- =========================
-- interests 제약조건
-- =========================
ALTER TABLE interests
    ADD CONSTRAINT UK_INTERESTS_NAME UNIQUE (name);
ALTER TABLE interests
    ADD CONSTRAINT CK_INTERESTS_SUBSCRIBER_COUNT CHECK (subscriber_count >= 0);

-- =========================
-- keywords 제약조건
-- =========================
ALTER TABLE keywords
    ADD CONSTRAINT UK_KEYWORDS_NAME UNIQUE (name);

-- =========================
-- interest_keywords 제약조건
-- =========================
ALTER TABLE interest_keywords
    ADD CONSTRAINT FK_INTEREST_KEYWORDS_INTEREST_ID FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE;
ALTER TABLE interest_keywords
    ADD CONSTRAINT FK_INTEREST_KEYWORDS_KEYWORD_ID FOREIGN KEY (keyword_id) REFERENCES keywords (id) ON DELETE CASCADE;
ALTER TABLE interest_keywords
    ADD CONSTRAINT UK_INTEREST_KEYWORDS_INTEREST_ID_KEYWORD_ID UNIQUE (interest_id, keyword_id);

-- =========================
-- user_activity_outbox 제약조건
-- =========================
-- 재시도 횟수는 음수가 될 수 없다.
ALTER TABLE user_activity_outbox
    ADD CONSTRAINT "CK_USER_ACTIVITY_OUTBOX_RETRY_COUNT"
        CHECK (retry_count >= 0);

-- 처리 상태는 미처리, 처리 완료, 처리 실패 상태만 허용한다.
ALTER TABLE user_activity_outbox
    ADD CONSTRAINT "CK_USER_ACTIVITY_OUTBOX_STATUS"
        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'));

-- 미처리 이벤트를 발생 시각 순으로 빠르게 조회하기 위한 인덱스
CREATE INDEX "IDX_USER_ACTIVITY_OUTBOX_STATUS_OCCURRED_AT"
    ON user_activity_outbox (status, occurred_at);
