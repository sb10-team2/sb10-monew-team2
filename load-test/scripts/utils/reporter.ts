export function generateCustomHtmlReport(data: any): string {
  const metrics = data.metrics;
  const apiStats: Record<string, any[]> = { 'warm-up': [], 'load-test': [] };

  for (const [key, value] of Object.entries(metrics)) {
    if (key.startsWith('http_req_duration{name:[')) {
      const match = key.match(/name:\[(.*?)\] (.*)}/);
      if (match) {
        const scenario = match[1];
        const apiName = match[2];

        const reqsKey = `http_reqs{name:[${scenario}] ${apiName}}`;
        const failsKey = `http_req_failed{name:[${scenario}] ${apiName}}`;

        const duration = value as any;
        const reqs = metrics[reqsKey] as any || { values: { count: 0, rate: 0 } };
        const fails = metrics[failsKey] as any || { values: { rate: 0 } };

        if (apiStats[scenario]) {
          apiStats[scenario].push({
            name: apiName,
            avg: duration.values.avg.toFixed(2),
            p95: duration.values['p(95)'].toFixed(2),
            p99: duration.values['p(99)'].toFixed(2),
            rps: reqs.values.rate.toFixed(2),
            errorRate: (fails.values.rate * 100).toFixed(2),
          });
        }
      }
    }
  }

  let html = `
    <html>
      <head>
        <title>Monew 성능 테스트 리포트</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; background: #f4f7f6; }
          details { background: white; padding: 15px; margin-bottom: 10px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
          summary { font-size: 1.2em; font-weight: bold; cursor: pointer; color: #2c3e50; }
          table { width: 100%; border-collapse: collapse; margin-top: 15px; }
          th, td { padding: 10px; border: 1px solid #ddd; text-align: center; }
          th { background-color: #34495e; color: white; }
          .error { color: red; font-weight: bold; }
        </style>
      </head>
      <body>
        <h1>🚀 성능 테스트 최종 리포트</h1>
  `;

  for (const [scenario, stats] of Object.entries(apiStats)) {
    if (stats.length === 0) continue;

    html += `<details>
              <summary>▶ ${scenario.toUpperCase()} 단계 상세 지표 보기</summary>
              <table>
                <tr>
                  <th>구분 (API)</th><th>평균 응답시간 (ms)</th><th>p95 (ms)</th><th>p99 (ms)</th><th>RPS / TPS</th><th>Error Rate (%)</th>
                </tr>`;

    for (const stat of stats) {
      const errorStyle = parseFloat(stat.errorRate) > 0 ? 'class="error"' : '';
      html += `<tr>
                <td>${stat.name}</td>
                <td>${stat.avg}</td>
                <td>${stat.p95}</td>
                <td>${stat.p99}</td>
                <td>${stat.rps}</td>
                <td ${errorStyle}>${stat.errorRate}%</td>
              </tr>`;
    }

    html += `</table></details>`;
  }

  html += `</body></html>`;
  return html;
}
