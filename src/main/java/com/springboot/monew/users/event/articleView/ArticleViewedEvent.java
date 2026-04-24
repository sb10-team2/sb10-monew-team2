package com.springboot.monew.users.event.articleView;

import com.springboot.monew.users.document.UserActivityDocument.ArticleViewItem;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID userId,
    ArticleViewItem item
) {

}
