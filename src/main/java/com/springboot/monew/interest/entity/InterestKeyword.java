package com.springboot.monew.interest.entity;

import com.springboot.monew.base.BaseEntity;
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
        name = "interest_keywords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_INTEREST_KEYWORDS_INTEREST_ID_KEYWORD_ID",
                        columnNames = {"interest_id", "keyword_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    public InterestKeyword(Interest interest, Keyword keyword) {
        this.interest = interest;
        this.keyword = keyword;
    }
}
