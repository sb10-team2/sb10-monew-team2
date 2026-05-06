import {sleep} from 'k6';
import {signin, signup} from '@/api/auth.api';
import {toUserDto} from "@/dto/user.dto";
import {generateSignupRequest} from "@/utils/data-factory";
import {createComment, getComments} from "@/api/comment.api";
import {readArticles} from "@/api/article.api";
import {randomThinkTime} from "@/utils/random";
import config from "@/config";
import {post} from "@/utils/http-client";
import {shuffleArray} from "@/utils/common";
import {readNotifications} from "@/api/notification.api";
import {createInterests, readInterests} from "@/api/interest.api";

export function readLoadTestScenario(articleIds: string[]): void {
  doLoadTestScenario(articleIds);
  sleep(0.5);
}

function doLoadTestScenario(articleIds: string[]): void {
  // sign-up
  const requestBody = generateSignupRequest();
  signup(requestBody);
  sleep(0.5);

  // sign-in
  const res = signin(requestBody);
  sleep(0.5);

  const userDto = toUserDto(res);
  const userId = userDto.id;
  let remainingComments = userDto.maxComments;
  let remainingLikes = userDto.maxLikes;

  const shuffledArticleIds = shuffleArray(articleIds);

  for (const articleId of shuffledArticleIds) {
    if (remainingComments <= 0 && remainingLikes <= 0) {
      break;
    }
    readArticles(userDto);

    if (remainingComments > 0) {
      createComment(articleId, userId);
      remainingComments--;
      randomThinkTime(0.5, 1.5);
    }

    if (remainingLikes > 0) {
      const consumedLikes = processCommentLikes(articleId, userId, remainingLikes);
      remainingLikes -= consumedLikes;
      randomThinkTime(1, 3);
    }

    readNotifications(userDto);
    createInterests(userDto);
    readInterests(userDto);
  }
}

function processCommentLikes(articleId: string, userId: string, remainingLikes: number): number {
  if (remainingLikes <= 0) return 0;

  const commentResponse = getComments(articleId, userId).content;
  if (!commentResponse || commentResponse.length === 0) return 0;

  const likesToPress = Math.min(
      remainingLikes,
      commentResponse.length,
      Math.floor(Math.random() * 3) + 1
  );

  const targetComments = shuffleArray(commentResponse).slice(0, likesToPress);
  for (const comment of targetComments) {
    const likeUrl = config.endpoints.postCommentLike.replace('{commentId}', comment.id);
    post(likeUrl, {}, userId);
    randomThinkTime(0.5, 1.5);
  }

  return likesToPress;
}
