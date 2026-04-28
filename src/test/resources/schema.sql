CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(100) NOT NULL,
    nickname   VARCHAR(20)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(50) NOT NULL,
    subscriber_count BIGINT DEFAULT 0 NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP
);

CREATE TABLE keywords
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE news_articles
(
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    source        VARCHAR(100) NOT NULL,
    original_link VARCHAR(500) NOT NULL,
    title         VARCHAR(300) NOT NULL,
    published_at  TIMESTAMP NOT NULL,
    summary       CLOB NOT NULL,
    view_count    BIGINT DEFAULT 0 NOT NULL,
    is_deleted    BOOLEAN NOT NULL
);

CREATE TABLE comments
(
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    article_id UUID NOT NULL,
    content    VARCHAR(200) NOT NULL,
    like_count BIGINT DEFAULT 0 NOT NULL,
    is_deleted BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    comment_id UUID NOT NULL,
    user_id    UUID NOT NULL
);

CREATE TABLE notifications
(
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP,
    confirmed        BOOLEAN DEFAULT false NOT NULL,
    content          VARCHAR(100) NOT NULL,
    resource_type    VARCHAR(10) NOT NULL,
    user_id          UUID NOT NULL,
    interest_id      UUID,
    comment_likes_id UUID
);

-- =========================
-- 제약조건
-- =========================

ALTER TABLE users
    ADD CONSTRAINT UK_USERS_EMAIL UNIQUE (email),
    ADD CONSTRAINT UK_USERS_NICKNAME UNIQUE (nickname);

ALTER TABLE interests
    ADD CONSTRAINT UK_INTERESTS_NAME UNIQUE (name),
    ADD CONSTRAINT CK_INTERESTS_SUBSCRIBER_COUNT CHECK (subscriber_count >= 0);

ALTER TABLE keywords
    ADD CONSTRAINT UK_KEYWORDS_NAME UNIQUE (name);

ALTER TABLE news_articles
    ADD CONSTRAINT UK_NEWS_ARTICLES_ORIGINAL_LINK UNIQUE (original_link),
    ADD CONSTRAINT CK_NEWS_ARTICLES_VIEW_COUNT CHECK (view_count >= 0);

ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENTS_USER_ID FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT FK_COMMENTS_NEWS_ARTICLE FOREIGN KEY (article_id) REFERENCES news_articles (id),
    ADD CONSTRAINT CK_COMMENTS_LIKE_COUNT CHECK (like_count >= 0);

ALTER TABLE comment_likes
    ADD CONSTRAINT FK_COMMENT_LIKES_USER_ID FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT FK_COMMENT_LIKES_COMMENT_ID FOREIGN KEY (comment_id) REFERENCES comments (id),
    ADD CONSTRAINT UQ_COMMENT_LIKES UNIQUE (comment_id, user_id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_USER_ID FOREIGN KEY (user_id) REFERENCES users (id),
    ADD CONSTRAINT FK_NOTIFICATIONS_INTEREST_ID FOREIGN KEY (interest_id) REFERENCES interests (id),
    ADD CONSTRAINT FK_NOTIFICATIONS_COMMENT_LIKES_ID FOREIGN KEY (comment_likes_id) REFERENCES comment_likes (id),
    ADD CONSTRAINT chk_notification_polymorphic_match
        CHECK (
            (resource_type = 'COMMENT' AND comment_likes_id IS NOT NULL AND interest_id IS NULL)
            OR
            (resource_type = 'INTEREST' AND interest_id IS NOT NULL AND comment_likes_id IS NULL)
        );