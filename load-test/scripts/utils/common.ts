import exec from "k6/execution";

export function getTag(method: string, uri: string): string {
  const scenario = exec.scenario.name || 'setup';
  return `[${scenario}] [${method} ${uri}]`;
}

export function shuffleArray<T>(array: T[]): T[] {
  return [...array].sort(() => Math.random() - 0.5);
}
