package com.springboot.monew.common.fixture;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.common.entity.BaseUpdatableEntity;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Model;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class EntityFixtureFactory {

  private static final Map<Class<?>, Model<?>> factory = new HashMap<>();

  static {
    factory.put(Notification.class, toModel(Notification.class));
    factory.put(CommentLike.class, toModel(CommentLike.class));
    factory.put(Comment.class, toModel(Comment.class));
    factory.put(NewsArticle.class, toModel(NewsArticle.class));
    factory.put(User.class, toModel(User.class));
    factory.put(Interest.class, toModel(Interest.class));
    factory.put(InterestKeyword.class, toModel(InterestKeyword.class));
    factory.put(Keyword.class, toModel(Keyword.class));
    factory.put(Subscription.class, toModel(Subscription.class));
    factory.put(ArticleView.class, toModel(ArticleView.class));
    factory.put(ArticleInterest.class, toModel(ArticleInterest.class));
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Class<T> type) {
    Model<?> model = factory.get(type);
    if (model == null) {
      throw new IllegalArgumentException("지원하지 않는 엔티티 타입입니다: " + type.getSimpleName());
    }
    return Instancio.create((Model<T>) model);
  }

  private static <T> Model<T> toModel(Class<T> type) {
    return Instancio.of(type)
        .supply(all(Notification.class), () -> Instancio.create(toNotificationWithCommentLikeModel())).lenient()
        .supply(all(CommentLike.class), () -> Instancio.create(toBaseModel(CommentLike.class))).lenient()
        .supply(all(Comment.class), () -> Instancio.create(toUpdatableModel(Comment.class))).lenient()
        .supply(all(NewsArticle.class), () -> Instancio.create(toBaseModel(NewsArticle.class))).lenient()
        .supply(all(User.class), () -> Instancio.create(toUpdatableModel(User.class))).lenient()
        .supply(all(Interest.class), () -> Instancio.create(toUpdatableModel(Interest.class))).lenient()
        .supply(all(InterestKeyword.class), () -> Instancio.create(toBaseModel(InterestKeyword.class))).lenient()
        .supply(all(Keyword.class), () -> Instancio.create(toBaseModel(Keyword.class))).lenient()
        .supply(all(Subscription.class), () -> Instancio.create(toBaseModel(Subscription.class))).lenient()
        .supply(all(ArticleView.class), () -> Instancio.create(toBaseModel(ArticleView.class))).lenient()
        .supply(all(ArticleInterest.class), () -> Instancio.create(toBaseModel(ArticleInterest.class))).lenient()
        .toModel();
  }

  public static Model<Notification> toNotificationWithCommentLikeModel() {
    return Instancio.of(Notification.class)
        .ignore(field(Notification::getId))
        .ignore(field(Notification::getCreatedAt))
        .ignore(field(Notification::getInterest))
        .set(field(Notification::getResourceType), ResourceType.COMMENT)
        .toModel();
  }

  public static Model<Notification> toNotificationWithInterestModel() {
    return Instancio.of(Notification.class)
        .ignore(field(Notification::getId))
        .ignore(field(Notification::getCreatedAt))
        .ignore(field(Notification::getCommentLike))
        .set(field(Notification::getResourceType), ResourceType.INTEREST)
        .toModel();
  }

  private static <T extends BaseEntity> Model<T> toBaseModel(Class<T> type) {
    return Instancio.of(type)
        .ignore(field(BaseEntity::getId))
        .ignore(field(BaseEntity::getCreatedAt))
        .toModel();
  }

  private static <T extends BaseUpdatableEntity> Model<T> toUpdatableModel(Class<T> type) {
    return Instancio.of(type)
        .ignore(field(BaseUpdatableEntity::getId))
        .ignore(field(BaseUpdatableEntity::getCreatedAt))
        .ignore(field(BaseUpdatableEntity::getUpdatedAt))
        .toModel();
  }
}
