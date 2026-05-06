package com.springboot.monew.common.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * 생성 및 수정 시간을 추적하는 엔티티 기반 클래스 - updatedAt: 수정 시점 자동 저장 - 수정이 발생하는 엔티티에서 상속 ex) Comment, User
 */

@MappedSuperclass
@Getter
public abstract class BaseUpdatableEntity extends BaseEntity {

  @LastModifiedDate
  private Instant updatedAt;
}
