package com.acme.tubeSurf.services;

import com.acme.tubeSurf.model.operation.CommentChunkRequest;
import com.acme.tubeSurf.model.operation.VideoCommentsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class CommentsJobProducer {
    private Logger logger = LoggerFactory.getLogger(CommentsJobProducer.class);

    @Autowired
    private BlockingQueue<CommentChunkRequest> urlQueue;
    @Autowired
    private CommentsChunkService commentsChunkService;

    @Async
    public void initVideoCommentsJob(VideoCommentsRequest videoCommentsRequest) {
        CommentChunkRequest commentChunkRequest = new CommentChunkRequest(
                videoCommentsRequest.getJobId(),
                videoCommentsRequest.getVideoId(),
                null,
                videoCommentsRequest.getCommentsInPage(),
                videoCommentsRequest.getTotalCommentsRequired(),
                0);
        logger.info("Init job for videoId: " + videoCommentsRequest.getVideoId() + " with jobId: " + videoCommentsRequest.getJobId());

        urlQueue.add(commentChunkRequest);
        commentsChunkService.consumeCommentsChunk();
    }

}
