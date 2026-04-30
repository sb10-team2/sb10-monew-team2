package com.springboot.monew.testdata;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.testdata.generator.ArticleInterestGenerator;
import com.springboot.monew.testdata.generator.ArticleViewGenerator;
import com.springboot.monew.testdata.generator.CommentGenerator;
import com.springboot.monew.testdata.generator.CommentLikeGenerator;
import com.springboot.monew.testdata.generator.InterestGenerator;
import com.springboot.monew.testdata.generator.InterestKeywordGenerator;
import com.springboot.monew.testdata.generator.KeywordGenerator;
import com.springboot.monew.testdata.generator.NewsArticleGenerator;
import com.springboot.monew.testdata.generator.NotificationGenerator;
import com.springboot.monew.testdata.generator.SubscriptionGenerator;
import com.springboot.monew.testdata.generator.UserGenerator;
import com.springboot.monew.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 부하 테스트를 위한 데이터 생성 클래스 application-test-data.yml test-data.generate.enabled=true로 바꿔야 데이터 생성된다
 * true 일 때 build 시 실행되므로 데이터 생성을 원치 않으면 반드시 false로 해야 한다 push 하기 전 enabled 를 확인해야 한다
 */
@Tag("test-data-generator")
@Import({TestDataConfig.class})
@ActiveProfiles("test-data")
@SpringBootTest
public class LoadTestDataRunnerTest {

  private static final int NUMBER_OF_ARTICLES = 50_000;
  private static final int NUMBER_OF_INTERESTS = 1000;
  private static final int NUMBER_OF_USERS = 1000;
  private static final int NUMBER_OF_KEYWORDS = 1000;

  @Autowired
  private UserGenerator userGenerator;
  @Autowired
  private NewsArticleGenerator newsArticleGenerator;
  @Autowired
  private InterestGenerator interestGenerator;
  @Autowired
  private KeywordGenerator keywordGenerator;
  @Autowired
  private InterestKeywordGenerator interestKeywordGenerator;
  @Autowired
  private ArticleViewGenerator articleViewGenerator;
  @Autowired
  private ArticleInterestGenerator articleInterestGenerator;
  @Autowired
  private SubscriptionGenerator subscriptionGenerator;
  @Autowired
  private CommentGenerator commentGenerator;
  @Autowired
  private CommentLikeGenerator commentLikeGenerator;
  @Autowired
  private NotificationGenerator notificationGenerator;
  @Autowired
  private JdbcTemplate template;

  private List<User> cachedUsers;
  private List<NewsArticle> cachedArticles;
  private List<Interest> cachedInterests;
  private List<Keyword> cachedKeywords;
  private List<Comment> cachedComments;
  private List<CommentLike> cachedCommentLikes;

  @BeforeEach
  void setUp() {
    String truncateSql = "TRUNCATE TABLE " +
        "\"interests\", \"users\", \"comment_likes\", \"article_interests\", " +
        "\"batch_step_execution_context\", \"notifications\", \"batch_job_execution\", " +
        "\"news_articles\", \"batch_step_execution\", \"batch_job_execution_context\", " +
        "\"batch_job_instance\", \"article_views\", \"subscriptions\", " +
        "\"batch_job_execution_params\", \"comments\", \"interest_keywords\", \"keywords\" " +
        "RESTART IDENTITY CASCADE;";

    template.execute(truncateSql);
  }

  @Test
  void generateAllData() {
    users();
    articles();
    interests();
    keywords();
    interestKeywordGenerator.setKeywordPerInterest(3);
    interestKeywords();
    articleViewGenerator.setArticlePerUser(10);
    articleViews();
    articleInterests();
    subscriptions();
    commentGenerator.setCommentPerUser(50);
    comments();
    commentLikeGenerator.setCommentLikePerUser(50);
    commentLikes();
    notificationGenerator.setNotificationPerUser(50);
    notifications();
  }

  @Test
  void userGenerator() {
    users();
  }

  @Test
  void newsArticleGenerator() {
    articles();
  }

  @Test
  void interestGenerator() {
    interests();
  }

  @Test
  void keywordGenerator() {
    keywords();
  }

  @Test
  void interestKeywordGenerator() {
    interestKeywordGenerator.setKeywordPerInterest(3);
    interestKeywords();
  }

  @Test
  void articleViewGenerator() {
    articleViewGenerator.setArticlePerUser(10);
    articleViews();
  }

  @Test
  void articleInterestGenerator() {
    articleInterests();
  }

  @Test
  void subscriptionGenerator() {
    subscriptions();
  }

  @Test
  void commentGenerator() {
    commentGenerator.setCommentPerUser(50);
    comments();
  }

  @Test
  void commentLikeGenerator() {
    commentLikeGenerator.setCommentLikePerUser(50);
    commentLikes();
  }

  @Test
  void notificationGenerator() {
    notificationGenerator.setNotificationPerUser(50);
    notifications();
  }

  // ====================================================================
  // 🛠️ 지연 초기화(Lazy Initialization)가 적용된 데이터 공급 메서드
  // ====================================================================

  private List<User> users() {
    if (cachedUsers == null) {
      cachedUsers = userGenerator.run(NUMBER_OF_USERS);
    }
    return cachedUsers;
  }

  private List<NewsArticle> articles() {
    if (cachedArticles == null) {
      cachedArticles = newsArticleGenerator.run(NUMBER_OF_ARTICLES);
    }
    return cachedArticles;
  }

  private List<Interest> interests() {
    if (cachedInterests == null) {
      cachedInterests = interestGenerator.run(NUMBER_OF_INTERESTS);
    }
    return cachedInterests;
  }

  private List<Keyword> keywords() {
    if (cachedKeywords == null) {
      cachedKeywords = keywordGenerator.run(NUMBER_OF_KEYWORDS);
    }
    return cachedKeywords;
  }

  private List<Comment> comments() {
    if (cachedComments == null) {
      cachedComments = commentGenerator.run(users(), articles());
    }
    return cachedComments;
  }

  private List<CommentLike> commentLikes() {
    if (cachedCommentLikes == null) {
      cachedCommentLikes = commentLikeGenerator.run(users(), comments());
    }
    return cachedCommentLikes;
  }

  // 💡 아래는 다른 엔티티에서 참조용으로 쓰이지 않는 최종 종착지 데이터들이므로 캐싱 불필요
  private List<Notification> notifications() {
    return notificationGenerator.run(users(), commentLikes(), interests());
  }

  private List<Subscription> subscriptions() {
    return subscriptionGenerator.run(users(), interests());
  }

  private List<ArticleInterest> articleInterests() {
    return articleInterestGenerator.run(interests(), articles());
  }

  private List<ArticleView> articleViews() {
    return articleViewGenerator.run(users(), articles());
  }

  private List<InterestKeyword> interestKeywords() {
    return interestKeywordGenerator.run(interests(), keywords());
  }
}
