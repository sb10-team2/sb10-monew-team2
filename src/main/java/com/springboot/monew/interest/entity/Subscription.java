package com.springboot.monew.interest.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_SUBSCRIPTIONS_USER_ID_INTEREST_ID",
            columnNames = {"user_id", "interest_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  public Subscription(User user, Interest interest) {
    this.user = user;
    this.interest = interest;
  }
}
