package com.springboot.monew.testdata;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.testdata.entity.ArticleInterestGenerator;
import com.springboot.monew.testdata.entity.ArticleViewGenerator;
import com.springboot.monew.testdata.entity.InterestGenerator;
import com.springboot.monew.testdata.entity.InterestKeywordGenerator;
import com.springboot.monew.testdata.entity.KeywordGenerator;
import com.springboot.monew.testdata.entity.NewsArticleGenerator;
import com.springboot.monew.testdata.entity.UserGenerator;
import com.springboot.monew.users.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 부하 테스트를 위한 데이터 생성 클래스
 *
 */
@Import({TestDataConfig.class})
@ActiveProfiles("test-data")
@SpringBootTest
public class LoadTestDataRunner {

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

  private List<ArticleInterest> articleInterests() {
    List<Interest> interests = interests();
    List<NewsArticle> articles = articles();
    return articleInterestGenerator.run(interests, articles);
  }

  private List<NewsArticle> articles() {
    return newsArticleGenerator.run(NUMBER_OF_ARTICLES);
  }

  private List<Interest> interests() {
    return interestGenerator.run(NUMBER_OF_INTERESTS);
  }

  private List<User> users() {
    return userGenerator.run(NUMBER_OF_USERS);
  }

  private List<Keyword> keywords() {
    return keywordGenerator.run(NUMBER_OF_KEYWORDS);
  }

  private List<ArticleView> articleViews() {
    List<User> users = users();
    List<NewsArticle> articles = articles();
    return articleViewGenerator.run(users, articles);
  }

  private List<InterestKeyword> interestKeywords() {
    List<Interest> interests = interests();
    List<Keyword> keywords = keywords();
    return interestKeywordGenerator.run(interests, keywords);
  }
}
