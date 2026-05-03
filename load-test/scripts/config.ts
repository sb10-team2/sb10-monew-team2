const baseUrl = "http://localhost:8080";
const apiPrefix = "/api";
const baseApi = `${baseUrl}${apiPrefix}`;

export const config = {
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
    vus: 100,
    duration: "10m"
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
