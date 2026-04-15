package com.springboot.monew.comment.service;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    // TODO : Article, User 구현 시 주석 해제
    // private final ArticleRepository articleRepository;
    // private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    // 댓글 등록
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
        return commentMapper.toCommentDto(comment, false);
    }
}
