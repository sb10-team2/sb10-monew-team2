package com.springboot.monew.common.entity;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.CommentLikeFixture;
import com.springboot.monew.common.fixture.InterestFixture;
import com.springboot.monew.common.fixture.UserFixture;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestEntityManager {

    @Autowired
    private EntityManager em;

    public Interest generateInterest() {
        return persistAndFlush(InterestFixture.createEntity());
    }

    public CommentLike generateCommentLike() {
        return persistAndFlush(CommentLikeFixture.createEntity());
    }

    public User generateUser() {
        return persistAndFlush(UserFixture.createEntity());
    }

    private <T> T persistAndFlush(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }
}
