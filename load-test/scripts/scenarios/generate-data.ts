import {generateSignupRequest} from "@/utils/data-factory";
import {signin, signup} from "@/api/auth.api";
import exec from "k6/execution";
import {createUserDto} from "@/dto/user.dto";
import config from "@/config";
import {get, post} from "@/utils/http-client";
import {CommentResponse, CursorResponse} from "@/types/api.type";

const LIMITS = {
  MAX_COMMENTS_PER_ARTICLE: 30,
  MAX_LIKES_PER_COMMENT: 10,
};

export function generateDataScenario(articleIds: string[]): void {
  // sign-up
  const requestBody = generateSignupRequest();
  signup(requestBody);

  // sign-in
  const res = signin(requestBody);

  const userDto = createUserDto(res);

  const shuffledArticles = [...articleIds].sort(() => Math.random() - 0.5);

  for (const articleId of shuffledArticles) {
    const commentsToCreate = Math.floor(Math.random() * LIMITS.MAX_COMMENTS_PER_ARTICLE) + 1;

    for (let i = 0; i < commentsToCreate; i++) {
      const commentBody = {
        articleId: articleId,
        userId: `dummy-user-${exec.vu.idInTest}-${i}`,
        content: `부하테스트용 더미 댓글 데이터입니다.`.padEnd(50, 'A')
      };
      post(config.endpoints.postComment, commentBody);
    }

    const url = `${config.endpoints.getComment}?articleId=${articleId}&limit=${LIMITS.MAX_COMMENTS_PER_ARTICLE}`;
    const commentsPage = get<CursorResponse<CommentResponse>>(url, userDto.id);
    const comments = commentsPage.content;

    for (const comment of comments) {
      const likesToPress = Math.floor(Math.random() * LIMITS.MAX_LIKES_PER_COMMENT);

      for (let j = 0; j < likesToPress; j++) {
        const likeUrl = config.endpoints.postCommentLike.replace('{commentId}', comment.id);
        const dummyLikerId = `liker-${exec.vu.idInTest}-${j}`;

        post(likeUrl, {}, dummyLikerId);
      }
    }
  }
}
