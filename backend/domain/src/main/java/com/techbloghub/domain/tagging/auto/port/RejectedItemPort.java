package com.techbloghub.domain.tagging.auto.port;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;

public interface RejectedItemPort {

    void saveRejectedItems(Post post, TaggingResult result);
}