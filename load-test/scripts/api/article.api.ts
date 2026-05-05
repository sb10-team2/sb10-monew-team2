import {ArticleResponse} from "@/types/api.type";
import {get} from "@/utils/http-client";
import config from "@/config";
import {getTag} from "@/utils/common";

export function getArticle(articleId: string, userId: string): ArticleResponse {
  const tag = getTag("GET", config.endpoints.getArticle);
  return get<ArticleResponse>(config.endpoints.getArticle.replace("{articleId}", articleId), userId, tag);
}
