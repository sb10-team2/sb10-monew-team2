import {CursorResponse, NotificationResponse} from "@/types/api.type";
import {get} from "@/utils/http-client";
import {getTag} from "@/utils/common";
import config from "@/config";
import {UserDto} from "@/dto/user.dto";
import {randomThinkTime} from "@/utils/random";

export function readNotifications(userDto: UserDto): void {
  const tag = getTag("GET", config.endpoints.getNotification);
  let limit = Math.floor(Math.random() * userDto.minNotifications) + 1
  let cursor = '';
  let after = '';
  let url = `${config.endpoints.getNotification}?limit=${limit}&cursor=${cursor}&after=${after}`;
  let hasNext: boolean = true;
  while (hasNext) {
    const response = get<CursorResponse<NotificationResponse>>(url, userDto.id, tag);
    hasNext = response.hasNext;
    cursor = response.nextCursor;
    after = response.nextAfter;
    limit = Math.floor(Math.random() * userDto.minNotifications) + 1
    url = `${config.endpoints.getNotification}?limit=${limit}&cursor=${cursor}&after=${after}`;
    randomThinkTime(0.5, 1.5);
  }
}
