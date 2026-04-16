package com.springboot.monew.comment.entity;

import com.springboot.monew.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "comment_likes")
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    // TODO: User 구현 완료 시 주석 해제
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id", nullable = false)
    // private User user;

    public CommentLike(Comment comment
                       // , User user
                       ) {
        this.comment = comment;
        // this.user = user;
    }
}
