package com.springboot.monew.interest.mapper;

import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.entity.Subscription;
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
}
