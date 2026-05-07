import {CommentLikeResponse} from "@/types/api.type";
import {UserDto} from "@/dto/user.dto";
import {getComments} from "@/api/comment.api";
import {post} from "@/utils/http-client";
import config from "@/config";
import {getTag} from "@/utils/common";

function createCommentLike(commentId: string, userId: string) {
  const url = config.endpoints.postCommentLike.replace("{commendId}", commentId);
  const tag = getTag(config.tags.postCommentLike);
  post<CommentLikeResponse>(url, null, userId, tag);
}
