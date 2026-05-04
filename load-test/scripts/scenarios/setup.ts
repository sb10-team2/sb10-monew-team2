import config from '@/config';
import {ArticleResponse, CursorResponse} from "@/types/api.type";
import {get} from "@/utils/http-client";

export default function (): string[] {
  const url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=100`;
  const response = get<CursorResponse<ArticleResponse>>(url);
  return response.content.map(article => article.id)
}
