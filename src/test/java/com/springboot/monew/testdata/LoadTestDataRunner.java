package com.springboot.monew.testdata;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
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

  @Test
  void userGenerator() {
    List<User> users = userGenerator.run(1000);
  }

  @Test
  void newsArticleGenerator() {
    List<NewsArticle> articles = newsArticleGenerator.run(50_000);
  }

  @Test
  void interestGenerator() {
    List<Interest> interests = interestGenerator.run(1000);
  }

  @Test
  void keywordGenerator() {
    List<Keyword> keywords = keywordGenerator.run(1000);
  }

  @Test
  void interestKeywordGenerator() {
    List<Interest> interests = interestGenerator.run(1000);
    List<Keyword> keywords = keywordGenerator.run(1000);
    interestKeywordGenerator.setKeywordPerInterest(3);
    List<InterestKeyword> interestKeywords = interestKeywordGenerator.run(interests, keywords);
  }

  @Test
  void articleViewGenerator() {
    List<User> users = userGenerator.run(1000);
    List<NewsArticle> articles = newsArticleGenerator.run(50_000);
    articleViewGenerator.setArticlePerUser(10);
    List<ArticleView> articleViews = articleViewGenerator.run(users, articles);
  }
}
