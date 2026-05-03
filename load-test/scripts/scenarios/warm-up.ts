import http from 'k6/http';
import {sleep} from 'k6';
import exec from 'k6/execution';
import {faker} from 'https://esm.sh/@faker-js/faker@10.3.0';
import {config} from '../config.ts';

interface PersonaData {
  ratio: number;
  maxComments: number;
  maxLikes: number;
  maxSubscriptions: number;
}

interface PersonaInfo {
  type: 'heavy' | 'light';
  data: PersonaData;
}

// 서버로 보낼 회원가입 요청 Body 타입
interface SignUpRequestBody {
  nickname: string;
  email: string;
  password: string;
  bio: string;
}

export function runWarmUp(articleIds: string[]): void {
  const vuId: number = exec.vu.idInTest;
  const iterId: number = exec.vu.iterationInScenario;

  const personaInfo: PersonaInfo = getPersonaInfo(vuId);
  const reqBody: SignUpRequestBody = createRequestBody(personaInfo.type, vuId, iterId);

  http.post(config.endpoints.postUser, JSON.stringify(reqBody), {
    headers: {'Content-Type': 'application/json'}
  });

  sleep(0.5);
}

function getPersonaInfo(vuId: number): PersonaInfo {
  const totalVu: number = config.warmUp.vus;
  const heavyRatio: number = config.persona.heavy.ratio;

  const heavyVuThreshold: number = totalVu * (heavyRatio / 100);

  if (vuId <= heavyVuThreshold) {
    return {type: 'heavy', data: config.persona.heavy};
  }
  return {type: 'light', data: config.persona.light};
}

function createRequestBody(type: 'heavy' | 'light', vuId: number, iterId: number): SignUpRequestBody {
  return {
    nickname: `${type}_${vuId}_${iterId}`,
    email: `${type}_${vuId}_${iterId}@monew.com`,
    password: 'password1234!',
    bio: faker.person.bio()
  };
}
