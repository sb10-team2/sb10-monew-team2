import {k6Options} from '@/config';

import runSetup from '@/scenarios/setup';
import {generateDataScenario} from '@/scenarios/generate-data';
import {readLoadTestScenario} from '@/scenarios/load-test';

import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import {generateCustomHtmlReport} from '@/utils/reporter'; // STEP 3에서 만들 파일

export const options = k6Options;

export function setup(): string[] {
  return runSetup();
}

export function runDataGeneration(articleIds: string[]) {
  generateDataScenario(articleIds);
}

export function runReadLoadTest(articleIds: string[]) {
  readLoadTestScenario(articleIds);
}

export function teardown(articleIds: string[]) {
  console.log(`테스트 종료. 사용된 Article ID 개수: ${articleIds.length}`);
}

export function handleSummary(data: any) {
  console.log('커스텀 HTML 리포트를 생성합니다...');

  return {
    'stdout': textSummary(data, {indent: ' ', enableColors: true}),
    '/test/test-report.html': generateCustomHtmlReport(data),
  };
}
