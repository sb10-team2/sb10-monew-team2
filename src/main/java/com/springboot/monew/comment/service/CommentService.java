package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.comment.mapper.CommentLikeMapper;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.exception.comment.CommentErrorCode;
import com.springboot.monew.exception.comment.CommentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    // TODO : Article, User 구현 시 주석 해제
    // private final ArticleRepository articleRepository;
    // private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;

    // 댓글 등록
    @Transactional
    public CommentDto create(CommentRegisterRequest request){
        // TODO: Article, User 구현 시 주석 해제
        // 존재하는 기사인지 check
        // Article article = articleRepository.findById(request.articleId())
        // .orElseThrow(커스텀 예외);
        // 존재하는 user인지 check
        // User user = userRepository.findById(request.userId())
        // .orElseThrow(커스텀 예외);
        // TODO: 논리 삭제 check

        // TODO: Article, User 추가
        Comment comment = new Comment(request.content());
        commentRepository.save(comment);
        log.info("댓글 등록 완료 - commentId: {}, articleId: {}, userId: {}", comment.getId(), request.articleId(), request.userId());
        return commentMapper.toCommentDto(comment, false);
    }

    // 댓글 수정
    @Transactional
    public CommentDto update(UUID commentId, UUID userId, CommentUpdateRequest request){
        // comment 조회 (존재, SoftDelete 여부 체크)
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));

        // user 조회 (존재, SoftDelete 여부 체크)

        // TODO: user 본인이 단 댓글인 지 확인, User 구현 시 주석 해제
        // if (!comment.getUser().getId().equals(userId)){
        // throw new CommentException(CommentErrorCode.COMMENT_NOT_OWNED_BY_USER, Map.of("commentId", commentId));
        // }

        // update 하고 Dto 변환
        comment.updateContent(request.content());
        boolean likeByMe = false;
                // TODO: 임시 값 false 해제 commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        log.info("댓글 수정 완료 - commentId: {} userId: {}", commentId, userId);
        log.debug("댓글 수정 완료 - commentId: {}, userId: {}, contentLength: {}",
                commentId, userId, comment.getContent().length());
        return commentMapper.toCommentDto(comment,likeByMe);
    }

    // 댓글 좋아요
    @Transactional
    public CommentLikeDto like(UUID commentId, UUID userId){
        // 댓글 조회 (존재, SoftDelete 여부)
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));

        // Todo: User 조회 (존재, SoftDelete 여부)
        // User user = userRepository.findByIdAndDeleteAt~(userId)

        // Todo: 중복 좋아요 check
//        if(commentLikeRepository.existsByIdAndUserId(commentId, userId)){
//            throw new CommentException(CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS,
//                    Map.of("commentId", commentId, "userId", userId));
//        }

        // CommentLike 생성 후 save && Comment의 likeCount ++
        // Todo: User 구현 시 로직 수정
        CommentLike commentLike = new CommentLike(comment);
        commentLikeRepository.save(commentLike);
        log.debug("likeCount 증가 전 - commentId: {}, likeCount: {}", commentId, comment.getLikeCount());
        commentRepository.incrementLikeCount(comment.getId());
        log.debug("likeCount 증가 후 - commentId: {}, likeCount: {}", commentId, comment.getLikeCount());

        log.info("좋아요 등록 완료 - commentId: {}, userId: {}", commentId, userId);

        // Comment, CommentLike -> CommentLikeDto로 변환
        return commentLikeMapper.toCommentLikeDto(commentLike);
    }

    // 댓글 논리 삭제
    @Transactional
    public void softDelete(UUID commentId) {
        // 조회 -> 존재하지 않는 경우, 이미 논리 삭제한 경우
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(
                        CommentErrorCode.COMMENT_NOT_FOUND,
                        Map.of("commentId", commentId))
                );
        if (comment.isDeleted()) {
            throw new CommentException(
                    CommentErrorCode.COMMENT_ALREADY_DELETED,
                    Map.of("commentId", commentId)
            );

        }
        // 논리 삭제 (false -> true)
        comment.delete();
        log.info("댓글 논리 삭제 완료 - commentId: {}", commentId);
    }

    // 댓글 물리 삭제
    @Transactional
    public void hardDelete(UUID commentId) {
        // 조회 - 소프트 딜릿 여부에 상관없이 삭제
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(
                        CommentErrorCode.COMMENT_NOT_FOUND,
                        Map.of("commentId", commentId))
                );

        commentRepository.delete(comment);
        log.info("댓글 물리 삭제 완료 - commentId: {}", commentId);
    }

    // 댓글 좋아요 취소
    @Transactional
    public void unlike(UUID commentId, UUID userId) {
        // 댓글 -> 존재, 소프트 딜릿 여부 확인
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(
                        CommentErrorCode.COMMENT_NOT_FOUND,
                        Map.of("commentId", commentId)
                ));

        // Todo: User -> 존재, 소프트 딜릿 여부 확인
        // User user = userRepository.findByIdAndDeletedAt~(userId);

        // Todo : 좋아요 존재 확인 (comment, user), 로직 수정해야 함.
        CommentLike commentLike = commentLikeRepository.findCommentLikeByComment(comment)
                .orElseThrow(
                        () -> new CommentException(
                                CommentErrorCode.COMMENT_LIKE_NOT_FOUND,
                                Map.of("commentId", commentId)
                        )
                );

        // 좋아요 삭제(하드 딜릿) -> 좋아요 취소 시 이력에서 바로 삭제되므로 하드딜릿이 맞다고 판단
        commentLikeRepository.delete(commentLike);

        // Comment 쪽 likeCount 감소 (동시성 문제 고려)
        commentRepository.decrementLikeCount(comment.getId());
        log.info("좋아요 취소 완료 - commentId: {}, userId: {}", commentId, userId);
    }
}
