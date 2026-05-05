import http from 'k6/http';
import {ApiException} from "@/exception/api.exception";

export function post<T>(url: string, body: any, userId?: string | null, tag?: string): T {
  const payload = body ? JSON.stringify(body) : '';
  const params: any = {
    headers: getHeader(userId),
  };
  if (tag) {
    params.tags = { name: tag };
  }
  const res = http.post(url, payload, params);
  if (res.status === 200 || res.status === 201) {
    return (res.body ? res.json() : null) as unknown as T;
  }
  console.error(`[API 에러] status: ${res.status},
  message: ${res.body},
  request: ${res.request.url}, headers: ${res.headers}`);
  throw new ApiException(res.status, res.body?.toString() || 'Unknown Error');
}

export function get<T>(url: string, userId?: string | null, tag?: string): T {
  const params: any = {
    headers: getHeader(userId),
  };
  if (tag) {
    params.tags = { name: tag };
  }
  const res = http.get(url, params);
  if (res.status === 200) {
    return res.json() as unknown as T;
  }
  console.error(`[API 에러] status: ${res.status},
  message: ${res.body},
  request: ${res.request.url}, headers: ${res.error}`);
  throw new ApiException(res.status, res.body?.toString() || 'Unknown Error');
}

function getHeader(userId?: string | null): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  };
  if (userId) {
    headers['Monew-Request-User-ID'] = userId;
  }
  return headers;
}
