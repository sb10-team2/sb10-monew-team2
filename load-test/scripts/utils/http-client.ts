import http from 'k6/http';
import {ApiException} from "@/exception/api.exception";

export function post<T>(url: string, body: any, userId?: string): T {
  const headers = getHeader(userId);
  const res = http.post(url, JSON.stringify(body), {headers});
  if (res.status === 200 || res.status === 201) {
    return res.json() as unknown as T;
  }
  console.error(`API 에러: ${res.status}, ${res.body}`);
  throw new ApiException(res.status, res.body?.toString() || 'Unknown Error');
}

export function get<T>(url: string): T {
  const res = http.get(url);
  if (res.status === 200) {
    return res.json() as unknown as T;
  }
  console.error(`API 에러: ${res.status}, ${res.body}`);
  throw new ApiException(res.status, res.body?.toString() || 'Unknown Error');
}

function getHeader(userId?: string): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  };
  if (userId) {
    headers['Monew-Request-User-ID'] = userId;
  }
  return headers;
}
