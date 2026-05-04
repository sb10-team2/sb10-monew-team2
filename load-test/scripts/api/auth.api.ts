import config from '@/config';
import {post} from '@/utils/http-client';
import {LoginRequest, SignupRequest, UserResponse} from "@/types/api.type";

export function signup(request: SignupRequest): UserResponse {
  return post<UserResponse>(config.endpoints.postUser, JSON.stringify(request));
}

export function signin(request: LoginRequest): UserResponse {
  return post<UserResponse>(config.endpoints.login, JSON.stringify(request));
}
