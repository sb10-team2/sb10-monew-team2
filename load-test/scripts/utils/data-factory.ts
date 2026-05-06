import exec from 'k6/execution';
import {SignupRequest} from "@/types/api.type";
import config from "@/config";

export function getPersonaType(): string {
  const vuId: number = exec.vu.idInTest;
  const totalVu: number = config.warmUp.vus;
  const heavyRatio: number = config.persona.heavy.ratio;
  const heavyVuThreshold: number = totalVu * heavyRatio;
  return vuId <= heavyVuThreshold ? 'heavy' : 'light';
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
