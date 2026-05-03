import http from 'k6/http';
import {config} from '../config';

export function setup() {
  let url = `${config.endpoints.getArticles}?orderBy=publishDate&direction=DESC&limit=100`;
  let response = http.get(url);
  if (response.status !== 200) {
    console.error(`error: ${response.status}, message: ${response.body}`);
    return {articleIds: []};
  }
  const responseBody = response.json();
  return {articleIds: responseBody.content.map(article => article.id)}
}
