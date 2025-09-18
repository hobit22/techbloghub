package com.techbloghub.domain.tagging.auto.port;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;

public interface TaggingProcessorPort {

    TaggingResult processTagging(Post post);
}