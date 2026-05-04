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
  const vuId: number = exec.vu.idInTest;
  const iterId: number = exec.vu.iterationInScenario;
  const type: string = getPersonaType();

  return {
    nickname: `${type}_${vuId}_${iterId}`,
    email: `${type}_${vuId}_${iterId}@monew.com`,
    password: 'password1234!',
  };
}
