package com.acme.tubeSurf.repositories;

import com.acme.tubeSurf.model.output.ConciseComment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConciseCommentRepository extends MongoRepository<ConciseComment, String> {
    ConciseComment findConciseCommentByTopLevelCommentId(String id);
}
