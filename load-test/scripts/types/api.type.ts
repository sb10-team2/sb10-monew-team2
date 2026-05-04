export interface CursorResponse<T> {
  content: T[];
  hasNext: boolean;
  nextCursor: string;
  nextAfter: string;
  size: number;
  totalElements: number;
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
