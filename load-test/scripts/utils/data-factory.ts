import exec from 'k6/execution';
import {SignupRequest} from "@/types/api.type";
import config from "@/config";

export function getPersonaType(): string {
  const vuId: number = exec.vu.idInTest; // 1, 2, 3, 4, 5... (1부터 시작)
  const ratio: number = config.persona.heavy.ratio; // 0.2
  const cyclePosition = (vuId - 1) % 100;
  const threshold = ratio * 100;
  return cyclePosition < threshold ? 'heavy' : 'light';
}

export function generateSignupRequest(): SignupRequest {
  const type: string = getPersonaType();
  const phase: string = exec.scenario.name;
  const uuid = crypto.randomUUID();
  const nickname = generateRandomString(20);
  return {
    nickname: `${nickname}`,
    email: `${phase}_${type}_${uuid}@monew.com`,
    password: 'password1234!',
  };
}

export function generateRandomString(length: number): string {
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  const charactersLength = characters.length;

  for (let i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }

  return result;
}
