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
  return {
    nickname: `${uuid}`,
    email: `${phase}_${type}_${uuid}@monew.com`,
    password: 'password1234!',
  };
}
