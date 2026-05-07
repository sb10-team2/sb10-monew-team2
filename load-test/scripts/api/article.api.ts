import {
  ArticleResponse,
  ArticleSourceResponse,
  ArticleViewResponse,
  CursorResponse
} from "@/types/api.type";
import {get, post} from "@/utils/http-client";
import config from "@/config";
import {getTag} from "@/utils/common";
import {UserDto} from "@/dto/user.dto";
import {randomThinkTime} from "@/utils/random";

export function readArticles(userDto: UserDto): void {
  const tag = getTag(config.tags.getArticles);
  let cursor = '';
  let after = '';
  let hasNext: boolean = true;
  let count = 3;

  while (hasNext && count > 0) {
    const limit = Math.floor(Math.random() * userDto.minArticles) + 1;
    let url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=${limit}`;
    if (cursor) {
      url += `&cursor=${encodeURIComponent(cursor)}`;
    }
    if (after) {
      url += `&after=${encodeURIComponent(after)}`;
    }
    const response = get<CursorResponse<ArticleResponse>>(url, userDto.id, tag);
    if (!response || !response.content) {
      console.error("[readArticles] API 응답이 없어 루프를 종료합니다. URL:", url);
      break;
    }
    count--;
    hasNext = response.hasNext;
    cursor = response.nextCursor || '';
    after = response.nextAfter || '';
    randomThinkTime(3, 5);
  }
}

export function readArticle(articleId: string, userId: string): void {
  const tag = config.tags.getArticle;
  const url = config.endpoints.getArticle.replace("{articleId}", articleId);
  get<ArticleResponse>(url, userId, tag);
  createArticleView(articleId, userId);
}

function createArticleView(articleId: string, userId: string): void {
  const tag = config.tags.postArticleView;
  const url = config.endpoints.postArticleView.replace("{articleId}", articleId);
  post<ArticleViewResponse>(url, {}, userId, tag);
  randomThinkTime(1, 3);
}

export function readArticleSource(): void {
  const tag = config.tags.getSource;
  get<ArticleSourceResponse>(config.endpoints.getSource, null, tag);
  randomThinkTime(0.1, 0.5);
}
