package com.springboot.monew.interest.mapper;

import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionDtoMapper {

  @Mapping(source = "subscription.id", target = "id")
  @Mapping(source = "subscription.interest.id", target = "interestId")
  @Mapping(source = "subscription.interest.name", target = "interestName")
  @Mapping(source = "interestKeywords", target = "interestKeywords")
  @Mapping(source = "interestSubscriberCount", target = "interestSubscriberCount")
  @Mapping(source = "subscription.createdAt", target = "createdAt")
  SubscriptionDto toSubscriptionDto(Subscription subscription, List<String> interestKeywords,
      long interestSubscriberCount);

  // Subscription 엔티티와 관심사 부가 정보를 사용자 활동 문서에 저장할 구독 활동 Item으로 변환한다.
  @Mapping(source = "subscription.id", target = "id")
  @Mapping(source = "subscription.interest.id", target = "interestId")
  @Mapping(source = "subscription.interest.name", target = "interestName")
  @Mapping(source = "keywords", target = "interestKeywords")
  @Mapping(source = "subscription.createdAt", target = "createdAt")
  SubscriptionItem toSubscriptionItem(Subscription subscription, List<String> keywords);
}
