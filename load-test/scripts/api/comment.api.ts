import {post} from "@/utils/http-client";
import config from "@/config";
import {UserDto} from "@/dto/user.dto";
import {CommentCreateRequest} from "@/types/api.type";
import {CommentDto} from "@/dto/comment.dto";
import exec from 'k6/execution';

function createComment(articleId: string, userId: string): void {
  const requestBody = getCommentCreateRequest(articleId, userId);
  post<CommentDto>(config.endpoints.postComment, requestBody);
}

function getCommentCreateRequest(articleId: string, userId: string): CommentCreateRequest {
  return {
    articleId: articleId,
    userId: userId,
    content: generateCommentContent()
  };
}

export default function createComments(userDto: UserDto, articleIds: string[]) {
  for (let i = 0; i < userDto.maxComments; i++) {
    for (const articleId of articleIds) {
      createComment(articleId, userDto.id);
    }
  }
}

function generateCommentContent(): string {
  const vuId = exec.vu.idInTest;
  const iterId = exec.vu.iterationInScenario;
  const prob = Math.random();
  const length = getContentLength(prob);
  const baseText = `V${vuId}-I${iterId}-L${length}:`;
  return baseText.padEnd(length, "A");
}

function getContentLength(prob: number): number {
  if (prob < 0.7) {
    return Math.floor(Math.random() * 20) + 30;
  }
  if (prob < 0.95) {
    return Math.floor(Math.random() * 50) + 50;
  }
  // comment-content-max-length: 200
  return 200;
}
