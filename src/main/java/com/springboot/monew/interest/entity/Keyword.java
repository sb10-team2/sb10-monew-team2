package com.springboot.monew.interest.entity;

import com.springboot.monew.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "keywords",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_KEYWORDS_NAME",
            columnNames = "name"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

  @Column(name = "name", nullable = false, length = 100)
  String name;

  public Keyword(String name) {
    this.name = name;
  }
}
