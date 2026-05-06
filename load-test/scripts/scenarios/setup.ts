import config from '@/config';
import {ArticleResponse, CursorResponse, SignupRequest, UserResponse} from "@/types/api.type";
import {get, post} from "@/utils/http-client";

export default function (): string[] {
  const userResponse = getUser();
  const userId = userResponse.id;
  const url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=100`;
  const response = get<CursorResponse<ArticleResponse>>(url, userId, "[setup] [GET /api/articles]");
  return response.content.map(article => article.id);
}

function getUser(): UserResponse {
  const request: SignupRequest = {
    email: 'asdasd@asd.com',
    nickname: 'asdasd',
    password: '1q2w3e4r!'
  }
  try {
    return post<UserResponse>(config.endpoints.login, request, null, "[setup] [POST /api/users/login]");
  } catch (ApiException) {
    return post<UserResponse>(config.endpoints.postUser, request, null, "[setup] [POST /api/users]");
  }
}
