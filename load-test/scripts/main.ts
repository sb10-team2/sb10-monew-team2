import {Options} from 'k6/options';
import config from '@/config';

import runSetup from '@/scenarios/setup';
import runWarmUp from '@/scenarios/warm-up';

export const options: Options = {
  scenarios: {
    warmup_scenario: {
      executor: 'constant-vus',
      vus: config.warmUp.vus,
      duration: config.warmUp.duration,
    },
  },
};

export function setup(): string[] {
  return runSetup();
}

export default function (articleIds: string[]) {
  runWarmUp(articleIds);
  // runLoadTest(articleIds);
}

export function teardown(articleIds: string[]) {
  console.log(`테스트 종료. 사용된 Article ID 개수: ${articleIds.length}`);
}
