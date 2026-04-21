package com.springboot.monew.interest.mapper;

import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.entity.Interest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterestDtoMapper {

  @Mapping(source = "interest.id", target = "id")
  @Mapping(source = "interest.name", target = "name")
  @Mapping(source = "keywords", target = "keywords")
  @Mapping(source = "interest.subscriberCount", target = "subscriberCount")
  @Mapping(source = "subscribedByMe", target = "subscribedByMe")
  InterestDto toInterestDto(Interest interest, List<String> keywords, boolean subscribedByMe);
}
