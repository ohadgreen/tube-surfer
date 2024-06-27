package com.acme.tubeSurf.repositories;

import com.acme.tubeSurf.model.output.ConciseComment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConciseCommentRepository extends MongoRepository<ConciseComment, String> {
    ConciseComment findConciseCommentByTopLevelCommentId(String id);

    List<ConciseComment> findConciseCommentByJobId(String jobId);
}
