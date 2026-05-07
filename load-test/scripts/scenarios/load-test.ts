import {sleep} from 'k6';
import {signin, signup} from '@/api/auth.api';
import {toUserDto, UserDto} from "@/dto/user.dto";
import {generateSignupRequest} from "@/utils/data-factory";
import {createComment, getComments} from "@/api/comment.api";
import {readArticle, readArticles, readArticleSource} from "@/api/article.api";
import {randomThinkTime} from "@/utils/random";
import config from "@/config";
import {post} from "@/utils/http-client";
import {getTag, shuffleArray} from "@/utils/common";
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
    readArticleSource();
    readArticlesRandomly(articleId, userDto);

    if (remainingComments > 0) {
      const consumed = createCommentsRandomly(articleId, userId, remainingComments);
      remainingComments -= consumed;
    }

    if (remainingLikes > 0) {
      const consumedLikes = createCommentLikesRandomly(articleId, userId, remainingLikes);
      remainingLikes -= consumedLikes;
    }

    readNotifications(userDto);
    createInterests(userDto);
    readInterests(userDto);
  }
}

function readArticlesRandomly(articleId: string, userDto: UserDto) {
  if (Math.random() < 0.5) {
    readArticles(userDto);
    return;
  }
  readArticle(articleId, userDto.id);
}

function createCommentsRandomly(articleId: string, userId: string, remainingComments: number): number {
  if (remainingComments <= 0) return 0;
  const count = Math.min(remainingComments, Math.floor(Math.random() * 3) + 1);
  for (let i = 0; i < count; i++) {
    createComment(articleId, userId);
  }
  return count;
}

function createCommentLikesRandomly(articleId: string, userId: string, remainingLikes: number): number {
  if (remainingLikes <= 0) return 0;

  const commentResponse = getComments(articleId, userId).content;
  if (!commentResponse || commentResponse.length === 0) return 0;

  const likesToPress = Math.min(
      remainingLikes,
      commentResponse.length,
      Math.floor(Math.random() * 3) + 1
  );

  const tag = getTag(config.tags.postCommentLike);
  const targetComments = shuffleArray(commentResponse).slice(0, likesToPress);
  for (const comment of targetComments) {
    const likeUrl = config.endpoints.postCommentLike.replace('{commentId}', comment.id);
    post(likeUrl, {}, userId, tag);
    randomThinkTime(0.5, 1.0);
  }

  return likesToPress;
}
