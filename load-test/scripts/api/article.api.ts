import {ArticleResponse, CursorResponse} from "@/types/api.type";
import {get} from "@/utils/http-client";
import config from "@/config";
import {getTag} from "@/utils/common";
import {UserDto} from "@/dto/user.dto";
import {randomThinkTime} from "@/utils/random";

export function readArticles(userDto: UserDto): void {
  const tag = getTag("GET", config.endpoints.getArticles);
  let cursor = '';
  let after = '';
  let hasNext: boolean = true;

  while (hasNext) {
    // 매 루프마다 limit을 새로 뽑습니다.
    const limit = Math.floor(Math.random() * userDto.minArticles) + 1;

    // 🌟 핵심 1: 필수 파라미터만 먼저 세팅
    let url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=${limit}`;

    // 🌟 핵심 2: 값이 존재할 때만 파라미터를 추가하고, 반드시 URL 인코딩을 거칩니다.
    if (cursor) {
      url += `&cursor=${encodeURIComponent(cursor)}`;
    }
    if (after) {
      url += `&after=${encodeURIComponent(after)}`;
    }

    const response = get<CursorResponse<ArticleResponse>>(url, userDto.id, tag);

    // K6에서 400 에러 등이 발생해 response가 undefined로 오면 무한 루프 방지를 위해 탈출
    if (!response) {
      console.error("[readArticles] API 응답이 없어 루프를 종료합니다. URL:", url);
      break;
    }

    // 다음 페이지 준비 (null/undefined일 경우 빈 문자열로 처리)
    hasNext = response.hasNext;
    cursor = response.nextCursor || '';
    after = response.nextAfter || '';

    randomThinkTime(0.5, 1.5);
  }
}
