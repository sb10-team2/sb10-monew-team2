import config from "@/config";

export function generateReport(data: any): string {
  try {
    const metrics = data?.metrics || {};
    const scenarios: Record<string, any[]> = {};
    let hasDetails = false;

    for (const [key, value] of Object.entries(metrics)) {
      if (key.startsWith('http_req_duration{') && key.includes('name:[')) {
        hasDetails = true;
        const match = key.match(/name:\[(.*?)\]\s+\[(.*?)\]/);

        if (match) {
          const scenarioName = match[1];
          const apiName = match[2];

          if (!scenarios[scenarioName]) scenarios[scenarioName] = [];

          const duration = (value as any)?.values || {};
          const reqs = (metrics[key.replace('http_req_duration', 'http_reqs')] as any)?.values || {};
          const fails = (metrics[key.replace('http_req_duration', 'http_req_failed')] as any)?.values || {};

          scenarios[scenarioName].push({
            api: apiName,
            avg: duration.avg || 0,
            p95: duration['p(95)'] || 0,
            p99: duration['p(99)'] || 0,
            count: reqs.count || 0,
            tps: reqs.rate || 0,
            errorRate: fails.rate || 0,
          });
        }
      }
    }

    let filteredTotalReqs = 0;
    let filteredTotalTps = 0;
    let filteredErrorCount = 0;
    let filteredP99Sum = 0;
    let apiCount = 0;

    for (const [name, apis] of Object.entries(scenarios)) {
      if (name === 'data_generation') continue;

      apis.forEach(api => {
        filteredTotalReqs += api.count;
        filteredTotalTps += api.tps;
        filteredErrorCount += (api.count * api.errorRate);
        filteredP99Sum += api.p99;
        apiCount++;
      });
    }

    const vusMax = metrics.vus_max?.values?.value || 0;
    const totalReqs = filteredTotalReqs;
    const globalTps = filteredTotalTps.toFixed(2);
    const failedRate = totalReqs > 0 ? ((filteredErrorCount / totalReqs) * 100).toFixed(2) : "0.00";
    const p99Duration = apiCount > 0 ? (filteredP99Sum / apiCount).toFixed(2) : "0.00";

    let apiRows = "";

    if (!hasDetails) {
      apiRows = `<tr><td colspan="8" class="error">상세 API 태그 데이터가 없습니다. thresholds 설정을 확인하세요.</td></tr>`;
    } else {

      const sortedScenarios = Object.entries(scenarios).sort(([nameA], [nameB]) => {
        if (nameA === 'load_test') return -1; // load_test를 제일 앞으로
        if (nameB === 'load_test') return 1;
        return 0;
      });

      for (const [scenario, apis] of sortedScenarios) {
        const activeApis = apis.filter(api => api.count > 0);
        if (activeApis.length === 0) continue;

        const totalCount = activeApis.reduce((sum, api) => sum + api.count, 0);
        const totalTps = activeApis.reduce((sum, api) => sum + api.tps, 0);

        let totalErrors = 0, sumAvg = 0, sumP95 = 0, sumP99 = 0;
        activeApis.forEach(api => {
          totalErrors += api.count * api.errorRate;
          sumAvg += api.avg; sumP95 += api.p95; sumP99 += api.p99;
        });
        const avgErrorRate = totalCount > 0 ? (totalErrors / totalCount) : 0;

        let displayScenarioName = scenario;
        if (scenario === 'load_test') {
          displayScenarioName = `load_test (VU: ${config.scenarios.loadTest.vus})`;
        }

        apiRows += `
          <tr class="summary-row" onclick="toggleDetails('${scenario}')" title="클릭하여 세부 API 보기">
            <td style="text-align: left; padding-left: 15px;">▶ <b>${displayScenarioName}</b></td>
            <td><b>* (통합 지표)</b></td>
            <td style="color: #8e44ad;"><b>${totalCount}</b></td>
            <td><b>${(sumAvg / activeApis.length).toFixed(2)}</b></td>
            <td><b>${(sumP95 / activeApis.length).toFixed(2)}</b></td>
            <td><b>${(sumP99 / activeApis.length).toFixed(2)}</b></td>
            <td class="highlight-tps"><b>${totalTps.toFixed(2)}</b></td>
            <td class="${avgErrorRate > 0 ? 'error' : 'success'}"><b>${(avgErrorRate * 100).toFixed(2)}%</b></td>
          </tr>
        `;

        for (const api of activeApis) {
          apiRows += `
            <tr class="detail-row-${scenario}">
              <td style="color: #7f8c8d; text-align: right;">└─</td>
              <td style="text-align: left;">${api.api}</td>
              <td style="color: #8e44ad; font-weight: bold;">${api.count}</td>
              <td>${api.avg.toFixed(2)}</td>
              <td>${api.p95.toFixed(2)}</td>
              <td>${api.p99.toFixed(2)}</td>
              <td class="highlight-tps">${api.tps.toFixed(2)}</td>
              <td class="${api.errorRate > 0 ? 'error' : ''}">${(api.errorRate * 100).toFixed(2)}%</td>
            </tr>
          `;
        }
      }
    }

    return `
      <!DOCTYPE html>
      <html lang="ko">
        <head>
          <meta charset="UTF-8">
          <title>Monew 성능 테스트 최종 리포트</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; background: #f4f7f6; color: #333; }
            h1 { text-align: center; color: #2c3e50; }
            details { background: white; padding: 20px; margin-bottom: 20px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
            summary { font-size: 1.3em; font-weight: bold; cursor: pointer; color: #34495e; outline: none; }
            table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 0.95em; }
            th, td { padding: 12px 15px; border: 1px solid #e0e6ed; text-align: center; }
            th { background-color: #34495e; color: white; position: sticky; top: 0; z-index: 10; }
            .summary-row { background-color: #e8f4f8; cursor: pointer; transition: background-color 0.2s; }
            .summary-row:hover { background-color: #d1eaf3; }
            .error { color: #e74c3c; font-weight: bold; }
            .success { color: #27ae60; font-weight: bold; }
            .highlight-tps { color: #2980b9; font-weight: bold; }
          </style>
          <script>
            function toggleDetails(scenario) {
              const rows = document.querySelectorAll('.detail-row-' + scenario);
              let isHidden = false;
              rows.forEach(row => {
                if (row.style.display === 'none') {
                  row.style.display = 'table-row';
                } else {
                  row.style.display = 'none';
                  isHidden = true;
                }
              });
              
              const summaryCell = event.currentTarget.cells[0];
              if (isHidden) {
                summaryCell.innerHTML = summaryCell.innerHTML.replace('▼', '▶');
              } else {
                summaryCell.innerHTML = summaryCell.innerHTML.replace('▶', '▼');
              }
            }
          </script>
        </head>
        <body>
          <h1>🚀 Monew 성능 테스트 결과 리포트</h1>
          
          <details open>
            <summary>📊 글로벌 핵심 요약 (Global Summary)</summary>
            <table>
              <tr>
                <th>최대 가상 유저 (VUs)</th>
                <th>총 요청 수 (Total Reqs)</th>
                <th>전체 TPS (RPS)</th>
                <th>글로벌 에러율 (Error Rate)</th>
                <th>글로벌 응답 속도 (p99 평균)</th>
              </tr>
              <tr>
                <td><b>${vusMax} 명</b></td>
                <td><b>${totalReqs} 건</b></td>
                <td class="highlight-tps"><b>${globalTps}</b></td>
                <td class="${Number(failedRate) > 0 ? 'error' : 'success'}"><b>${failedRate} %</b></td>
                <td><b>${p99Duration} ms</b></td>
              </tr>
            </table>
          </details>

          <details open>
            <summary>▶ 시나리오 및 API별 상세 지표 (상단 행 클릭 시 접기/펴기)</summary>
            <table>
              <tr>
                <th style="width: 18%;">시나리오 (Scenario)</th>
                <th style="width: 25%;">API (Method / URI)</th>
                <th>요청 횟수 (Count)</th>
                <th>평균 (ms)</th>
                <th>p95 (ms)</th>
                <th>p99 (ms)</th>
                <th>TPS / RPS</th>
                <th>Error Rate (%)</th>
              </tr>
              ${apiRows}
            </table>
          </details>
        </body>
      </html>
    `;
  } catch (e) {
    return `<html><meta charset="UTF-8"><body><h1>리포트 생성 중 에러 발생</h1><p style="color:red;">${String(e)}</p></body></html>`;
  }
}
