export interface CursorResponse<T> {
  content: T[];
  hasNext: boolean;
  nextCursor: string;
  nextAfter: string;
  size: number;
  totalElements: number;
}

export interface InterestResponse {

}

export interface NotificationResponse {
  id: string;
  createdAt: string;
  updatedAt: string;
  confirmed: boolean;
  userId: string;
  content: string;
  resourceType: string;
  resourceId: string;
}

export interface CommentLikeResponse {
  id: string;
  likedBy: string;
  createdAt: string;
  commentId: string;
  articleId: string;
  commentUserId: string;
  commentUserNickname: string;
  commentContent: string;
  commentLikeCount: string;
  commentCreatedAt: string;
}

export interface CommentResponse {
  id: string;
  articleId: string;
  userId: string;
  userNickname: string;
  content: string;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface ArticleResponse {
  id: string;
  source: string;
  sourceUrl: string;
  title: string;
  publishDate: string;
  summary: string;
  commentCount: number;
  viewCount: number;
  viewedByMe: boolean;
}

export interface CommentLikeCreateRequest {
  commentId: string;
  userId: string;
}

export interface CommentCreateRequest {
  articleId: string;
  userId: string;
  content: string;
}

export interface SignupRequest {
  email: string;
  nickname: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: string;
  email: string;
  nickname: string;
  createdAt: string;
}
