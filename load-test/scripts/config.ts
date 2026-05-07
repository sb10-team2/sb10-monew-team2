import {Options, Scenario} from "k6/options";

const baseUrl = "http://host.docker.internal:8080";
const apiPrefix = "/api";
const baseApi = `${baseUrl}${apiPrefix}`;

const config = {
  endpoints: {
    postUser: `${baseApi}/users`,
    login: `${baseApi}/users/login`,
    getComment: `${baseApi}/comments`,
    postComment: `${baseApi}/comments`,
    getUserActivity: `${baseApi}/user-activities`,
    postCommentLike: `${baseApi}/comments/{commentId}/comment-likes`,
    getInterest: `${baseApi}/interests`,
    postInterest: `${baseApi}/interests`,
    postSubscription: `${baseApi}/{interestId}/subscriptions`,
    getNotification: `${baseApi}/notifications`,
    getArticle: `${baseApi}/articles/{articleId}`,
    getArticles: `${baseApi}/articles`,
    getSource: `${baseApi}/articles/sources`,
    postArticleView: `${baseApi}/articles/{articleId}/article-views`
  },
  tags: {
    postUser: 'POST /api/users',
    login: 'POST /api/users/login',
    getComment: 'GET /api/comments',
    postComment: 'POST /api/comments',
    getUserActivity: 'GET /api/user-activities',
    postCommentLike: 'POST /api/comments/{commentId}/comment-likes',
    getInterest: 'GET /api/interests',
    postInterest: 'POST /api/interests',
    postSubscription: 'POST /api/{interestId}/subscriptions',
    getNotification: 'GET /api/notifications',
    getArticle: 'GET /api/articles/{articleId}',
    getArticles: 'GET /api/articles',
    getSource: 'GET /api/articles/sources',
    postArticleView: 'POST /api/articles/{articleId}/article-views'
  },
  scenarios: {
    dataGeneration: {
      vus: 50,
      iterations: 1000,
      durationMin: 10,
      limits: {maxLikesPerComment: 10, maxCommentsPerArticle: 30}
    },
    warmUp: {vus: 5, durationMin: 2},
    loadTest: {vus: 50, durationMin: 15},
  },
  persona: {
    heavy: {
      ratio: 0.2,
      maxComments: 100,
      maxLikes: 200,
      maxInterests: 30,
      maxKeywords: 10,
      minNotifications: 10,
      minArticles: 50,
    },
    light: {
      ratio: 0.8,
      maxComments: 5,
      maxLikes: 10,
      maxInterests: 3,
      maxKeywords: 1,
      minNotifications: 30,
      minArticles: 100,
    }
  }
};

export const k6Options: Options = {
  scenarios: buildScenarios(),
  thresholds: generateThresholds(),
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function generateLinearStages(targetVus: number, durationMin: number) {
  const rampUpMin = Math.max(1, Math.floor(durationMin * 0.3));
  const holdMin = Math.max(1, durationMin - (rampUpMin * 2));
  const rampDownMin = durationMin - rampUpMin - holdMin;

  return [
    {duration: `${rampUpMin}m`, target: targetVus},
    {duration: `${holdMin}m`, target: targetVus},
    {duration: `${rampDownMin}m`, target: 0},
  ];
}

function buildScenarios(): { [name: string]: Scenario } {
  let currentStartMin = 0;

  const dataGen = {
    executor: 'shared-iterations',
    startTime: `${currentStartMin}m`,
    vus: config.scenarios.dataGeneration.vus,
    iterations: config.scenarios.dataGeneration.iterations,
    maxDuration: `${config.scenarios.dataGeneration.durationMin}m`,
    exec: 'runDataGeneration',
  } as any;

  currentStartMin += config.scenarios.dataGeneration.durationMin;

  const warmUp = {
    executor: 'constant-vus',
    startTime: `${currentStartMin}m`,
    vus: config.scenarios.warmUp.vus,
    duration: `${config.scenarios.warmUp.durationMin}m`,
    exec: 'runReadLoadTest',
  } as any;

  currentStartMin += config.scenarios.warmUp.durationMin;

  const loadTest = {
    executor: 'ramping-vus',
    startTime: `${currentStartMin}m`,
    startVUs: config.scenarios.warmUp.vus,
    stages: generateLinearStages(config.scenarios.loadTest.vus, config.scenarios.loadTest.durationMin),
    exec: 'runReadLoadTest',
  } as any;

  return {
    data_generation: dataGen,
    warm_up: warmUp,
    load_test: loadTest
  };
}

function generateThresholds() {
  const dynamicThresholds: any = {};
  const scenarios = ['warm_up', 'load_test'];

  Object.values(config.tags).forEach(t => {
    scenarios.forEach(scenario => {
      const tag = `name:[${scenario}] [${t}]`;
      dynamicThresholds[`http_req_duration{${tag}}`] = ['p(95)>=0'];
      dynamicThresholds[`http_reqs{${tag}}`] = ['count>=0'];
      dynamicThresholds[`http_req_failed{${tag}}`] = ['rate>=0'];
    })
  });

  return dynamicThresholds;
}

export default config;
