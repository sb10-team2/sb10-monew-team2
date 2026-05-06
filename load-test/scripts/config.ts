import {Options} from "k6/options";

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
  setup: {
    ghostUser: 1000,
    article: 100,
  },
  data_generation: {
    vus: 50,
    iterations: 10000,
    maxDuration: '10m',
    limits: {
      maxCommentsPerArticle: 30,
      maxLikesPerComment: 10,
    },
  },
  warmUp: {
    vus: 5,
    duration: "2m"
  },
  loadTest: {
    vus: 500, duration: '10m'
  },
  persona: {
    heavy: {
      ratio: 0.2,
      maxComments: 100,
      maxLikes: 200,
      maxInterests: 30,
      maxKeywords: 20,
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
  scenarios: {
    data_generation: {
      executor: 'shared-iterations',
      vus: config.data_generation.vus,
      iterations: config.data_generation.iterations,
      maxDuration: config.data_generation.maxDuration,
      exec: 'runDataGeneration',
    },
    warm_up: {
      executor: 'constant-vus',
      startTime: '10m',
      vus: config.warmUp.vus,
      duration: config.warmUp.duration,
      exec: 'runReadLoadTest',
    },
    load_test: {
      executor: 'ramping-vus',
      startTime: '12m',
      startVUs: config.warmUp.vus,
      stages: [
        {duration: '1m', target: 50},
        {duration: '1m', target: 100},
        {duration: '1m', target: 300},
        {duration: '4m', target: 500},
        {duration: '1m', target: 300},
        {duration: '1m', target: 100},
        {duration: '1m', target: 0},
      ],
      exec: 'runReadLoadTest',
    },
  },
  thresholds: generateThresholds(),
};

function generateThresholds() {
  const dynamicThresholds: any = {};
  const scenarios = ['warm_up', 'load_test'];

  Object.entries(config.endpoints).forEach(([key, url]) => {
    const method = getHttpMethod(key);
    if (method !== "UNKNOWN") {
      scenarios.forEach(scenario => {
        const tag = `name:[${scenario}] [${method} ${url}]`;

        // 🌟 K6가 3대 지표를 절대 지우지 못하게 더미 임계값을 걸어 강제 보존합니다!
        dynamicThresholds[`http_req_duration{${tag}}`] = ['p(95)>=0'];
        dynamicThresholds[`http_reqs{${tag}}`] = ['count>=0'];
        dynamicThresholds[`http_req_failed{${tag}}`] = ['rate>=0'];
      })
    }
  });

  return dynamicThresholds;
}

function getHttpMethod(key: string): string {
  if (key.toLowerCase().startsWith('get')) {
    return "GET";
  }
  if (key.toLowerCase().startsWith('post')) {
    return "POST";
  }
  if (key.toLowerCase().startsWith('put')) {
    return "PUT";
  }
  if (key.toLowerCase().startsWith('delete')) {
    return "DELETE";
  }
  return "UNKNOWN";
}

export default config;
