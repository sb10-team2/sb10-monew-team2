package com.springboot.monew.base;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 생성 및 수정 시간을 추적하는 엔티티 기반 클래스
 * - updatedAt: 수정 시점 자동 저장
 * - 수정이 발생하는 엔티티에서 상속
 * ex) Comment, User
 */

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseUpdatableEntity extends BaseEntity {
    @LastModifiedDate
    private Instant updatedAt;
}
