package com.springboot.monew.common.fixture;

import static org.instancio.Select.field;

import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import java.util.List;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Model;

public class NotificationFixtureBuilder {

  private int size = 10;
  private ResourceType resourceType = ResourceType.INTEREST;
  private User user;

  private NotificationFixtureBuilder() {
  }

  public static NotificationFixtureBuilder builder() {
    return new NotificationFixtureBuilder();
  }

  public NotificationFixtureBuilder size(int size) {
    this.size = size;
    return this;
  }

  public NotificationFixtureBuilder resourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  public NotificationFixtureBuilder user(User user) {
    this.user = user;
    return this;
  }

  public Model<Notification> build() {
    return applyRules(Instancio.of(Notification.class)).toModel();
  }

  public Model<List<Notification>> buildList() {
    return applyRules(Instancio.ofList(Notification.class).size(size)).toModel();
  }

  private <T> InstancioApi<T> applyRules(InstancioApi<T> api) {
    api.ignore(field(Notification::getId))
        .ignore(field(Notification::getCreatedAt))
        .ignore(field(Notification::getUpdatedAt))
        .set(field(Notification::getConfirmed), false)
        .set(field(Notification::getResourceType), this.resourceType);

    if (this.user != null) {
      api.set(field(Notification::getUser), this.user);
    }

    if (this.resourceType == ResourceType.INTEREST) {
      return api.ignore(field(Notification::getCommentLike));
    }
    return api.ignore(field(Notification::getInterest));
  }
}
