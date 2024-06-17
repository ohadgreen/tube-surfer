package com.acme.tubeSurf.repositories;

import com.acme.tubeSurf.model.output.CommentsJobSummary;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentsJobSummaryRepository extends MongoRepository<CommentsJobSummary, String> {
    CommentsJobSummary findCommentsJobSummaryByJobId(String jobId);

}
