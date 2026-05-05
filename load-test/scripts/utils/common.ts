import exec from "k6/execution";

export function getTag(method: string, uri: string): string {
  const scenario = exec.scenario.name;
  return `[${scenario}] ${method} ${uri}`;
}
