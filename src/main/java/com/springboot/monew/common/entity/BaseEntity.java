package com.springboot.monew.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티의 공통 기반 클래스 - id: UUID 자동 생성 - createdAt: 생성 시점 자동 저장 (@EnableJpaAuditing 필요)
 */

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @CreatedDate
  @Column(updatable = false)
  private Instant createdAt;
}
