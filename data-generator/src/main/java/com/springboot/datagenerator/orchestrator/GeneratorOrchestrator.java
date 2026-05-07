package com.springboot.datagenerator.orchestrator;

import com.springboot.datagenerator.generator.ArticleInterestGenerator;
import com.springboot.datagenerator.generator.ArticleViewGenerator;
import com.springboot.datagenerator.generator.CommentGenerator;
import com.springboot.datagenerator.generator.CommentLikeGenerator;
import com.springboot.datagenerator.generator.InterestGenerator;
import com.springboot.datagenerator.generator.InterestKeywordGenerator;
import com.springboot.datagenerator.generator.KeywordGenerator;
import com.springboot.datagenerator.generator.NewsArticleGenerator;
import com.springboot.datagenerator.generator.NotificationGenerator;
import com.springboot.datagenerator.generator.SubscriptionGenerator;
import com.springboot.datagenerator.generator.UserGenerator;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.newsarticle.entity.ArticleInterest;
import com.springboot.monew.newsarticle.entity.ArticleView;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneratorOrchestrator {

  private final UserGenerator userGenerator;
  private final NewsArticleGenerator newsArticleGenerator;
  private final InterestGenerator interestGenerator;
  private final KeywordGenerator keywordGenerator;
  private final InterestKeywordGenerator interestKeywordGenerator;
  private final ArticleViewGenerator articleViewGenerator;
  private final ArticleInterestGenerator articleInterestGenerator;
  private final SubscriptionGenerator subscriptionGenerator;
  private final CommentGenerator commentGenerator;
  private final CommentLikeGenerator commentLikeGenerator;
  private final NotificationGenerator notificationGenerator;
  private final JdbcTemplate template;

  public void run() {
    truncated();

    List<User> users = userGenerator.run();
    List<NewsArticle> articles = newsArticleGenerator.run();
    List<Interest> interests = interestGenerator.run();
    List<Keyword> keywords = keywordGenerator.run();
    List<Comment> comments = commentGenerator.run(users, articles);
    List<CommentLike> commentLikes = commentLikeGenerator.run(users, comments);
    List<InterestKeyword> interestKeywords = interestKeywordGenerator.run(interests, keywords);
    List<ArticleView> articleViews = articleViewGenerator.run(users, articles);
    List<ArticleInterest> articleInterests = articleInterestGenerator.run(interests, articles);
    List<Subscription> subscriptions = subscriptionGenerator.run(users, interests);
    List<Notification> notifications = notificationGenerator.run(users, commentLikes, interests);
  }

  private void truncated() {
    String truncateSql = "TRUNCATE TABLE " +
        "\"interests\", \"users\", \"comment_likes\", \"article_interests\", " +
        "\"notifications\", " +
        "\"news_articles\", " +
        "\"article_views\", \"subscriptions\", " +
        "\"comments\", \"interest_keywords\", \"keywords\" " +
        "RESTART IDENTITY CASCADE;";

    template.execute(truncateSql);
  }
}
