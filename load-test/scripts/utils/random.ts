import {sleep} from 'k6';

export function randomThinkTime(minSeconds: number, maxSeconds: number) {
  const waitTime = Math.random() * (maxSeconds - minSeconds) + minSeconds;
  sleep(waitTime);
}
