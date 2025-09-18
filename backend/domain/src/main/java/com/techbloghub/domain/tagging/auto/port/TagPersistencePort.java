package com.techbloghub.domain.tagging.auto.port;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;

public interface TagPersistencePort {

    TaggingProcessStatus persistTaggingResult(Post post, TaggingResult result);
}