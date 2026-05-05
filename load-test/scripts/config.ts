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
  warmUp: {
    vus: 5,
    duration: "1m"
  },
  loadTest: {
    vus: 100, duration: '10m'
  },
  persona: {
    heavy: {
      ratio: 0.2,
      maxComments: 100,
      maxLikes: 200,
      maxSubscriptions: 100
    },
    light: {
      ratio: 0.8,
      maxComments: 5,
      maxLikes: 10,
      maxSubscriptions: 5
    }
  }
};

export const k6Options: Options = {
  scenarios: {
    data_generation: {
      executor: 'shared-iterations',
      vus: 50,
      iterations: 10000,
      maxDuration: '5m',
      exec: 'runDataGeneration',
    },
    warm_up: {
      executor: 'constant-vus',
      startTime: '5m',
      vus: config.warmUp.vus,
      duration: config.warmUp.duration,
      exec: 'runReadLoadTest',
    },
    load_test: {
      executor: 'ramping-vus',
      startTime: '6m',
      startVUs: config.warmUp.vus,
      stages: [
        {duration: '2m', target: config.loadTest.vus},
        {duration: config.loadTest.duration, target: config.loadTest.vus},
      ],
      exec: 'runReadLoadTest',
    },
  },
};

export default config;
