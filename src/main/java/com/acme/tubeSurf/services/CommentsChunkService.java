package com.acme.tubeSurf.services;

import com.acme.tubeSurf.model.operation.CommentChunkRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class CommentsChunkService {

    private Logger logger = LoggerFactory.getLogger(CommentsChunkService.class);

    @Autowired
    private BlockingQueue<CommentChunkRequest> urlQueue;
    @Autowired
    private CommentsChunkHandler commentsChunkHandler;

    @Async
    public void consumeCommentsChunk() {
        while (true) try {
            CommentChunkRequest commentChunkRequest = urlQueue.take();
            logger.info("Consuming job for videoId: " + commentChunkRequest.getVideoId() + " with jobId: " + commentChunkRequest.getJobId());
            commentsChunkHandler.receiveCommentChunk(commentChunkRequest);
            // process the comment chunk
        } catch (InterruptedException e) {
            logger.error("Error consuming comment chunk", e);
        }
    }

}
