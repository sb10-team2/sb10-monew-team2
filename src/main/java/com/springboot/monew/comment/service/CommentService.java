package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.exception.comment.CommentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    // TODO : Article, User 구현 시 주석 해제
    // private final ArticleRepository articleRepository;
    // private final UserRepository userRepository;
    private final CommentMapper commentMapper;

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
        // comment 조회 Todo: 일단 예외 보류
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        // user 본인이 단 댓글인 지 확인
        // commentRepository.existsCommentByUserId();

        // update 하고 Dto 변환
        comment.updateContent(request.content());

        return commentMapper.toCommentDto(comment, false);
    }
}
