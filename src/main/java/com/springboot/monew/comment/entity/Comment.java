package com.springboot.monew.comment.entity;

import com.springboot.monew.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Comment extends BaseUpdatableEntity {
    // TODO: User, Article 구현 완료 시 주석 해제 예정

    /*
        @ManyToOne(fetch = FetchType.LAZY)
        @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
        private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        @Column(name = "article_id", nullable = false, columnDefinition = "UUID")
        private Article article;
     */

    // 댓글 내용
    @Column(name = "content", nullable = false, length = 200)
    private String content;

    // 좋아요 수
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    // 삭제 여부 -> 논리 삭제
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public Comment(
            // User user,
            // Article article,
            String content
    ) {
        this.content = content;
        this.likeCount = 0;
        this.isDeleted = false;
    }

    // === 비즈니스 로직 ===
    // 메시지 내용 수정
    public void updateContent(String content) {
        this.content = content;
    }
    // 좋아요 수 증가
    public void increaseLikeCount() {
        this.likeCount++;
    }
    // 좋아요 수 감소
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
    // 논리 삭제 시
    public void delete() {
        this.isDeleted = true;
    }
}
