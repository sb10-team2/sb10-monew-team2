import config from '@/config';
import {post} from '@/utils/http-client';
import {LoginRequest, SignupRequest, UserResponse} from "@/types/api.type";
import {getTag} from "@/utils/common";

export function signup(request: SignupRequest): UserResponse {
  const tag = getTag("POST", config.endpoints.postUser);
  return post<UserResponse>(config.endpoints.postUser, JSON.stringify(request), null, tag);
}

export function signin(request: LoginRequest): UserResponse {
  const tag = getTag("POST", config.endpoints.postUser);
  return post<UserResponse>(config.endpoints.login, JSON.stringify(request), null, tag);
}
