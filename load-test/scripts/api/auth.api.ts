import config from '@/config';
import {post} from '@/utils/http-client';
import {LoginRequest, SignupRequest, UserResponse} from "@/types/api.type";
import {getTag} from "@/utils/common";
import {generateRandomString} from "@/utils/data-factory";

export function signup(request: SignupRequest): UserResponse {
  const tag = getTag("POST", config.endpoints.postUser);
  return post<UserResponse>(config.endpoints.postUser, request, null, tag);
}

export function signin(request: LoginRequest): UserResponse {
  const tag = getTag("POST", config.endpoints.postUser);
  return post<UserResponse>(config.endpoints.login, request, null, tag);
}

export function getGhostUserIds(num: number): string[] {
  const userIds: string[] = [];
  for (let i=0;i<num;i++) {
    userIds.push(createGhostUserId());
  }
  return userIds;
}

function createGhostUserId(): string {
  const request: SignupRequest = {
    email: `${crypto.randomUUID()}@ghost.user`,
    nickname: generateRandomString(20),
    password: '1q2w3e4r!'
  }
  const response = post<UserResponse>(config.endpoints.postUser, request, null);
  return response.id;
}
