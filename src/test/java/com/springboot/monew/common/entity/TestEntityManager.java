package com.springboot.monew.common.entity;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestEntityManager {

  @Autowired
  private EntityManager em;

  public Interest generateInterest() {
    return persistAndFlushWithRecursive(get(Interest.class));
  }

  public Notification generateNotification() {
    return persistAndFlushWithRecursive(get(Notification.class));
  }

  public CommentLike generateCommentLike() {
    return persistAndFlushWithRecursive(get(CommentLike.class));
  }

  public User generateUser() {
    return persistAndFlushWithRecursive(get(User.class));
  }

  public NewsArticle generateArticle() {
    return persistAndFlushWithRecursive(get(NewsArticle.class));
  }

  public User getProxyUser() {
    return em.getReference(User.class, UUID.randomUUID());
  }

  public Interest getProxyInterest() {
    return em.getReference(Interest.class, UUID.randomUUID());
  }

  public CommentLike getProxyCommentLike() {
    return em.getReference(CommentLike.class, UUID.randomUUID());
  }

  private <T> T persistAndFlushWithRecursive(T entity) {
    Set<Object> visited = new HashSet<>();
    try {
      return doPersistRecursive(entity, visited);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("persist recursively error", e);
    }
  }

  private <T> T doPersistRecursive(T object, Set<Object> visited) throws IllegalAccessException {
    if (object == null || visited.contains(object)) {
      return object;
    }
    Class<?> clazz = object.getClass();
    if (!clazz.isAnnotationPresent(Entity.class)) {
      return object;
    }
    visited.add(object);
    for (Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);
      Object fieldValue = field.get(object);
      if (fieldValue != null) {
        if (field.getType().isAnnotationPresent(Entity.class)) {
          doPersistRecursive(fieldValue, visited);
        }
      }
    }
    em.persist(object);
    em.flush();
    return object;
  }

  private <T> T get(Class<T> type) {
    return EntityFixtureFactory.get(type);
  }
}
