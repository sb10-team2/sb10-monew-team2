package com.springboot.monew.notification.service;

import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.common.utils.TimeConverter;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.notification.mapper.NotificationMapper;
import com.springboot.monew.notification.mapper.NotificationMapperImpl;
import com.springboot.monew.notification.repository.NotificationRepository;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Spy
  private final NotificationMapper mapper = new NotificationMapperImpl();
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private CommentLikeRepository commentLikeRepository;
  @Mock
  private InterestRepository interestRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private NotificationService service;

  @Test
  @DisplayName("관심사 알림 생성 이벤트를 이용해 알림 생성 및 저장하고 dto 반환")
  void successToCreateByInterest() {
    // given
    User user = EntityFixtureFactory.get(User.class);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    Interest interest = EntityFixtureFactory.get(Interest.class);
    ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
    InterestNotificationEvent event = new InterestNotificationEvent(interest.getId());
    Notification expected = Notification.builder()
        .resourceType(ResourceType.INTEREST)
        .user(user)
        .interest(interest)
        .build();
    NotificationDto expectedDto = mapper.toDto(expected);

    given(notificationRepository.saveAll(anyList())).willReturn(List.of(expected));
    given(subscriptionRepository.findUserIdsByInterestId(interest.getId())).willReturn(List.of(user.getId()));
    given(interestRepository.getReferenceById(any(UUID.class))).willReturn(interest);
    given(userRepository.getReferenceById(any(UUID.class))).willReturn(user);

    // when
    List<NotificationDto> actualDto = service.create(event);

    // then
    Assertions.assertThat(actualDto).isEqualTo(List.of(expectedDto));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
    verify(notificationRepository, times(1)).saveAll(captor.capture());

    List<Notification> actual = captor.getValue();
    Assertions.assertThat(actual).isNotEmpty().hasSize(1);
    Assertions.assertThat(actual.get(0))
        .extracting("user", "interest", "resourceType")
        .contains(expected.getUser(), expected.getInterest(), expected.getResourceType());
  }

  @Test
  @DisplayName("댓글 좋아요 알림 생성 이벤트를 이용해 알림 생성 및 저장 후 dto 반환")
  void successToCreateWithCommentLike() {
    // given
    User user = EntityFixtureFactory.get(User.class);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    ReflectionTestUtils.setField(commentLike, "id", UUID.randomUUID());
    CommentLikeNotificationEvent event = new CommentLikeNotificationEvent(user.getId(),
        commentLike.getId());
    Notification expected = Notification.builder()
        .resourceType(ResourceType.COMMENT)
        .user(user)
        .commentLike(commentLike)
        .build();
    NotificationDto expectedDto = mapper.toDto(expected);

    given(notificationRepository.save(any(Notification.class))).willReturn(expected);
    given(userRepository.getReferenceById(any(UUID.class))).willReturn(user);
    given(commentLikeRepository.getReferenceById(any(UUID.class))).willReturn(commentLike);

    // when
    NotificationDto actualDto = service.create(event);

    // then
    Assertions.assertThat(actualDto).isEqualTo(expectedDto);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository, times(1)).save(captor.capture());

    Notification actual = captor.getValue();
    Assertions.assertThat(actual)
        .extracting("user", "commentLike", "resourceType")
        .contains(expected.getUser(), expected.getCommentLike(), expected.getResourceType());
  }

  @Test
  @DisplayName("알람 객체 cursor pagination 조회 성공\n"
      + "hasNext=true, nextCursor, nextAfter를 반환한다")
  void successToFindByCursorAndHasNext() {
    // given
    int size = 3;
    long totalElements = 5L;
    boolean hasNext = true;
    User user = EntityFixtureFactory.get(User.class);
    UUID userId = user.getId();
    NotificationFindRequest request = new NotificationFindRequest(null, null, size);
    Pageable pageable = PageRequest.of(0, size);
    List<Notification> content = Instancio.ofList(Notification.class)
        .size(size)
        .ignore(field(Notification::getCommentLike))
        .ignore(field(Notification::getContent))
        .set(field(Notification::getResourceType), ResourceType.INTEREST)
        .set(field(Notification::getConfirmed), false)
        .set(field(Notification::getUser), user)
        .create();
    Slice<Notification> slice = new SliceImpl<>(content, pageable, hasNext);
    Notification last = content.get(size - 1);

    given(notificationRepository.findByCursor(any(), any(), eq(userId), any(Pageable.class))).willReturn(slice);
    given(notificationRepository.countAllByUser_IdAndConfirmedIsFalse(userId)).willReturn(totalElements);

    // when
    CursorPageResponse<NotificationDto> response = service.find(request, userId);

    // verify
    Assertions.assertThat(response)
        .extracting("size", "hasNext", "totalElements", "nextCursor", "nextAfter")
        .contains(size, hasNext, totalElements, last.getId().toString(),
            TimeConverter.toDatetime(last.getCreatedAt()).toString());
  }
}
