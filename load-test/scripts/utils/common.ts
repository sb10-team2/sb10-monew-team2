import exec from "k6/execution";

export function getTag(tag: string): string {
  const scenario = exec.scenario.name || 'setup';
  return `[${scenario}] [${tag}]`;
}

export function shuffleArray<T>(array: T[]): T[] {
  return [...array].sort(() => Math.random() - 0.5);
}
