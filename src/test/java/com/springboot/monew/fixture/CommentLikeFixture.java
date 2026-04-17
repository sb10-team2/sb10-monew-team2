package com.springboot.monew.fixture;

import com.springboot.monew.comment.entity.CommentLike;
import org.instancio.Instancio;

public final class CommentLikeFixture {
    private CommentLikeFixture() {
    }

    public static CommentLike createEntity() {
        return Instancio.create(CommentLike.class);
    }
}
