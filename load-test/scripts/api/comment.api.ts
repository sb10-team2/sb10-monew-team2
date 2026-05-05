import {get, post} from "@/utils/http-client";
import config from "@/config";
import {UserDto} from "@/dto/user.dto";
import {CommentCreateRequest, CommentResponse, CursorResponse} from "@/types/api.type";
import {CommentDto} from "@/dto/comment.dto";
import exec from 'k6/execution';
import {getTag} from "@/utils/common";

export function createComment(articleId: string, userId: string): void {
  const requestBody = getCommentCreateRequest(articleId, userId);
  const tag = getTag("POST", config.endpoints.postComment);
  post<CommentDto>(config.endpoints.postComment, requestBody, null, tag);
}

function getCommentCreateRequest(articleId: string, userId: string): CommentCreateRequest {
  return {
    articleId: articleId,
    userId: userId,
    content: generateCommentContent()
  };
}

export function getComments(articleId: string, userId: string): CursorResponse<CommentResponse> {
  const url = `${config.endpoints.getComment}?articleId=${articleId}&orderBy=createdAt&direction=DESC&limit=20`;
  const tag = getTag("POST", config.endpoints.postComment);
  return get<CursorResponse<CommentResponse>>(url, userId, tag);
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
