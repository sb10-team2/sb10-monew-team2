package com.springboot.monew.common.fixture;

import com.springboot.monew.comment.entity.CommentLike;

public final class CommentLikeFixture {
    private static final BaseFixture baseFixture = BaseFixture.INSTANT;

    private CommentLikeFixture() {
    }

    public static CommentLike createEntity() {
        return baseFixture.baseEntity(CommentLike.class).create();
    }
}
