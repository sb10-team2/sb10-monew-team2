package com.springboot.monew.user.event.articleView;

import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID userId,
    ArticleViewItem item
) {

}
