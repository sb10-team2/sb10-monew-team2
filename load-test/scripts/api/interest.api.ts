import {CursorResponse, InterestResponse, NotificationResponse} from "@/types/api.type";
import {UserDto} from "@/dto/user.dto";
import config from "@/config";
import {get, post} from "@/utils/http-client";
import {generateRandomString} from "@/utils/data-factory";
import {getTag} from "@/utils/common";
import {randomThinkTime} from "@/utils/random";

export function createInterests(userDto: UserDto): InterestResponse[] {
  const results: InterestResponse[] = [];
  const count = Math.floor(Math.random() * userDto.maxInterests) + 1;
  const url = config.endpoints.postInterest;
  const tag = getTag("POST", url);
  for (let i = 0; i < count; i++) {
    const requestBody = {
      name: generateRandomString(20),
      keywords: generateKeywords(userDto.maxKeywords)
    };
    const response = post<InterestResponse>(url, requestBody, userDto.id, tag);
    results.push(response);
    randomThinkTime(0.5, 1.5);
  }
  return results;
}

export function readInterests(userDto: UserDto): void {
  const tag = getTag("GET", config.endpoints.getInterest);
  let limit = Math.floor(Math.random() * userDto.maxInterests) + 1
  let cursor = '';
  let after = '';
  let url = `${config.endpoints.getInterest}?orderBy=name&direction=DESC&limit=${limit}&cursor=${cursor}&after=${after}`;
  let hasNext: boolean = true;
  while (hasNext) {
    const response = get<CursorResponse<InterestResponse>>(url, userDto.id, tag);
    hasNext = response.hasNext;
    cursor = response.nextCursor;
    after = response.nextAfter;
    limit = Math.floor(Math.random() * userDto.maxInterests) + 1
    url = `${config.endpoints.getInterest}?orderBy=name&direction=DESC&limit=${limit}&cursor=${cursor}&after=${after}`;
    randomThinkTime(0.5, 1.5);
  }
}

function generateKeywords(max: number): string[] {
  const keywords = [];
  const count = Math.floor(Math.random() * max) + 1;
  for (let i = 0; i < count; i++) {
    keywords.push(generateRandomString(100));
  }
  return keywords;
}
