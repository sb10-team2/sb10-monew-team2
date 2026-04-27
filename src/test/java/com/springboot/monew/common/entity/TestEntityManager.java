package com.springboot.monew.common.entity;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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

  public List<Notification> generateNotifications(int size) {
    return persistAndFlushWithRecursive(getList(Notification.class, size));
  }

  public CommentLike generateCommentLike() {
    return persistAndFlushWithRecursive(get(CommentLike.class));
  }

  public User generateUser() {
    return persistAndFlushWithRecursive(get(User.class));
  }

  public NewsArticle generateNewsArticle() {
    NewsArticle article = new NewsArticle(
        ArticleSource.NAVER,
        "https://test.com/" + UUID.randomUUID(),
        "테스트 기사 제목",
        Instant.now(),
        "테스트 요약"
    );
    em.persist(article);
    em.flush();
    return article;
  }

  public List<NewsArticle> generateNewsArticles(int size) {
    return persistAndFlushWithRecursive(getList(NewsArticle.class, size));
  }

  public Comment generateComment() {
    return persistAndFlushWithRecursive(get(Comment.class));
  }

  public List<Comment> generateComments(int size, NewsArticle article) {
    User sharedUser = generateUser();
    return persistAndFlushWithRecursive(
        EntityFixtureFactory.getCommentList(size, article, sharedUser));
  }

  public User getProxyUser() {
    return em.getReference(User.class, UUID.randomUUID());
  }

  private <T> T get(Class<T> type) {
    return EntityFixtureFactory.get(type);
  }

  private <T> List<T> getList(Class<T> type, int size) {
    return EntityFixtureFactory.getList(type, size);
  }

  private <T> T persistAndFlushWithRecursive(T object) {
    Set<Object> visited = new HashSet<>();
    try {
      doPersistRecursive(object, visited);
      em.flush();
      return object;
    } catch (IllegalAccessException e) {
      throw new RuntimeException("persist recursively error", e);
    }
  }

  private void doPersistRecursive(Object object, Set<Object> visited)
      throws IllegalAccessException {
    if (object == null || visited.contains(object)) {
      return;
    }
    if (object instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        doPersistRecursive(item, visited);
      }
      return;
    }
    Class<?> clazz = object.getClass();
    if (!clazz.isAnnotationPresent(Entity.class)) {
      return;
    }
    visited.add(object);
    Class<?> currentClazz = clazz;
    while (currentClazz != null && currentClazz != Object.class) {
      for (Field field : currentClazz.getDeclaredFields()) {
        field.setAccessible(true);
        Object fieldValue = field.get(object);

        if (fieldValue != null) {
          if (fieldValue.getClass().isAnnotationPresent(Entity.class)
              || fieldValue instanceof Iterable<?>) {
            doPersistRecursive(fieldValue, visited);
          }
        }
      }
      currentClazz = currentClazz.getSuperclass(); // 부모 클래스로 올라감
    }
    if (em.contains(object)) {
      return;
    }
    em.persist(object);
  }
}
