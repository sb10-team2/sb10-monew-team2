import {sleep} from 'k6';
import {signin, signup} from '@/api/auth.api';
import {createUserDto, UserDto} from "@/dto/user.dto";
import {generateSignupRequest} from "@/utils/data-factory";
import {createComment, getComments} from "@/api/comment.api";
import {getArticle} from "@/api/article.api";
import {randomThinkTime} from "@/utils/random";
import config from "@/config";
import {post} from "@/utils/http-client";

export function readLoadTestScenario(articleIds: string[]): void {
  // sign-up
  const requestBody = generateSignupRequest();
  signup(requestBody);
  sleep(0.5);

  // sign-in
  const res = signin(requestBody);
  sleep(0.5);

  const userDto = createUserDto(res);
  doLoadTestScenario(userDto, articleIds);
  sleep(0.5);
}

function doLoadTestScenario(userDto: UserDto, articleIds: string[]): void {
  const userId = userDto.id;
  let restOfComments = userDto.maxComments;
  let restOfCommentLikes = userDto.maxLikes;
  const shuffledArticleIds = [...articleIds].sort(() => Math.random() - 0.5);

  for (const articleId of shuffledArticleIds) {
    if (restOfCommentLikes == 0 && restOfComments == 0) {
      break;
    }

    getArticle(articleId, userId);
    randomThinkTime(3, 7);

    if (restOfComments > 0) {
      createComment(articleId, userId);
      restOfComments--;
      randomThinkTime(1, 3);
    }

    if (restOfCommentLikes > 0) {
      const commentCursorResponse = getComments(articleId, userId);
      const commentResponse = commentCursorResponse.content;

      if (commentResponse.length > 0) {
        const likesToPress = Math.min(
            restOfCommentLikes,
            commentResponse.length,
            Math.floor(Math.random() * 3) + 1
        );

        const targetComments = commentResponse.sort(() => Math.random() - 0.5).slice(0, likesToPress);

        for (const comment of targetComments) {
          const likeUrl = config.endpoints.postCommentLike.replace('{commentId}', comment.id);
          post(likeUrl, {}, userDto.id);
          restOfCommentLikes--;
          randomThinkTime(0.5, 1.5);
        }
      }
    }
  }
}
