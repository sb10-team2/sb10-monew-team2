import config from '@/config';
import {ArticleResponse, CursorResponse} from "@/types/api.type";
import {get} from "@/utils/http-client";

export default function (): string[] {
  const url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=100`;
  const response = get<CursorResponse<ArticleResponse>>(url, "14b624d0-0bde-4265-8d75-af5bae687377", "GET /api/articles?orderBy=publishDate&direction=DESC&limit=100");
  return response.content.map(article => article.id)
}
