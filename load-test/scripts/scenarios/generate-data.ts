import {generateSignupRequest} from "@/utils/data-factory";
import {getGhostUserIds, signin, signup} from "@/api/auth.api";
import {toUserDto} from "@/dto/user.dto";
import config from "@/config";
import {get, post} from "@/utils/http-client";
import {CommentResponse, CursorResponse} from "@/types/api.type";
import {shuffleArray} from "@/utils/common";

export function generateDataScenario(articleIds: string[]): void {
  // sign-up
  const requestBody = generateSignupRequest();
  signup(requestBody);

  // sign-in
  const res = signin(requestBody);
  const userDto = toUserDto(res);
  const userId = userDto.id;

  const shuffledArticles = shuffleArray(articleIds);
  const ghostUserIds = getGhostUserIds(config.data_generation.limits.maxLikesPerComment);
  const articleId = shuffledArticles[0];

  createCommentRandomly(articleId, userId);
  const url = `${config.endpoints.getComment}?articleId=${articleId}&orderBy=createdAt&direction=DESC&limit=${config.data_generation.limits.maxCommentsPerArticle}`;
  const commentsPage = get<CursorResponse<CommentResponse>>(url, userId);
  const comments = commentsPage.content;
  for (const comment of comments) {
    createCommentLikesRandomly(comment, ghostUserIds);
  }
}

function createCommentLikesRandomly(comment: CommentResponse, userIds: string[]) {
  const likesToPress = Math.floor(Math.random() * config.data_generation.limits.maxLikesPerComment) + 1
  const shuffledUserIds = shuffleArray(userIds);
  for (let j = 0; j < likesToPress; j++) {
    const likeUrl = config.endpoints.postCommentLike.replace('{commentId}', comment.id);
    const dummyLikerId = shuffledUserIds[j];
    post(likeUrl, {}, dummyLikerId);
  }
}

function createCommentRandomly(articleId: string, userId: string) {
  const commentsToCreate = Math.floor(Math.random() * config.data_generation.limits.maxCommentsPerArticle) + 1;
  const commentBody = {
    articleId: articleId,
    userId: userId,
    content: `부하테스트용 더미 댓글 데이터입니다.`.padEnd(50, 'A')
  };
  for (let i = 0; i < commentsToCreate; i++) {
    post(config.endpoints.postComment, commentBody);
  }
}
