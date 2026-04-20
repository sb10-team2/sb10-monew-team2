package com.springboot.monew.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.fixture.EntityFixtureFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.notification.mapper.NotificationMapper;
import com.springboot.monew.notification.mapper.NotificationMapperImpl;
import com.springboot.monew.notification.repository.NotificationRepository;
import com.springboot.monew.users.entity.User;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Spy
  private final NotificationMapper mapper = new NotificationMapperImpl();
  @Mock
  private NotificationRepository repository;
  @InjectMocks
  private NotificationService service;

  @Test
  @DisplayName("관심사에 의해 알람 생성 성공")
  void successToCreateWithInterest() {
    // given
    String content = "asd";
    ResourceType resourceType = ResourceType.INTEREST;
    User user = EntityFixtureFactory.get(User.class);
    Interest interest = EntityFixtureFactory.get(Interest.class);
    CommentLike commentLike = null;
    Notification entity = new Notification(content, resourceType, user, interest, commentLike);
    given(repository.save(any(Notification.class)))
        .willReturn(entity);

    // when
    service.create(content, resourceType, user, interest, commentLike);

    // verify
    verify(repository, times(1)).save(any(Notification.class));
  }

  @Test
  @DisplayName("댓글 좋아요에 의해 알람 생성 성공")
  void successToCreateWithCommentLike() {
    // given
    String content = "asd";
    ResourceType resourceType = ResourceType.COMMENT;
    User user = EntityFixtureFactory.get(User.class);
    Interest interest = null;
    CommentLike commentLike = EntityFixtureFactory.get(CommentLike.class);
    Notification entity = new Notification(content, resourceType, user, interest, commentLike);
    given(repository.save(any(Notification.class)))
        .willReturn(entity);

    // when
    service.create(content, resourceType, user, interest, commentLike);

    // verify
    verify(repository, times(1)).save(any(Notification.class));
  }

  @Test
  @DisplayName("알람 객체 조회 성공\n"
      + "전달 받은 parameter 수정하지 않고 pageable 생성하여 repository 전달")
  void successToFindByCursor() {
    // given
    NotificationFindRequest request = Instancio.create(NotificationFindRequest.class);
    UUID userId = UUID.randomUUID();
    given(repository.findByCursor(request.getCursor(), request.getAfter(), request.getUserId(),
        any(Pageable.class)))
        .willReturn(any());

    // when
    service.find(request, userId);

    // verify
    verify(repository, times(1)).findByCursor(request.getCursor(), request.getAfter(),
        request.getUserId(), any(Pageable.class));
  }

  @Test
  @DisplayName("한 유저의 확인하지 않은 모든 알람 중 단일 알람 확인 성공")
  void successToUpdateConfirmed() {

  }

  @Test
  @DisplayName("한 유저의 확인하지 않은 모든 알람 확인 성공")
  void successToBulkUpdateConfirmed() {

  }

}
