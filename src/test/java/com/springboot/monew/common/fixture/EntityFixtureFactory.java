package com.springboot.monew.common.fixture;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.common.entity.BaseUpdatableEntity;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.instancio.Instancio;
import org.instancio.Model;

// notice: Notification.class를 참조하는 entity가 있다면 exception 발생
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class EntityFixtureFactory {

  private static final Map<Class<?>, Model<?>> factory = new ConcurrentHashMap<>();

  public static <T> T get(Class<T> type) {
    return Instancio.create(getModel(type));
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> getList(Class<T> type, int size) {
    if (type == Notification.class) {
      User sharedUser = get(User.class);
      Model<List<Notification>> listModel = NotificationFixtureBuilder.builder()
          .size(size)
          .user(sharedUser)
          .buildList();
      return (List<T>) Instancio.create(listModel);
    }
    return Instancio.ofList(getModel(type))
        .size(size)
        .create();
  }

  @SuppressWarnings("unchecked")
  private static <T> Model<T> getModel(Class<T> type) {
    return (Model<T>) factory.computeIfAbsent(type, EntityFixtureFactory::createGlobalModel);
  }

  private static Model<?> createGlobalModel(Class<?> type) {
    return Instancio.of(type)
        .ignore(field(BaseEntity::getId))
        .ignore(field(BaseEntity::getCreatedAt))
        .ignore(field(BaseUpdatableEntity::getUpdatedAt))
        .supply(all(Notification.class), () -> Instancio.create(notificationWithInterestModel()))
        .lenient()
        .toModel();
  }

  private static Model<Notification> notificationWithInterestModel() {
    return NotificationFixtureBuilder.builder()
        .resourceType(ResourceType.INTEREST)
        .build();
  }
}
