export interface CommentDto {
  id: string;
  articleId: string;
  userId: string;
  userNickname: string;
  content: string;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
}
