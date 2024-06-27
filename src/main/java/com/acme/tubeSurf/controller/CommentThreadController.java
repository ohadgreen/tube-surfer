package com.acme.tubeSurf.controller;

import com.acme.tubeSurf.model.operation.CommentChunkRequest;
import com.acme.tubeSurf.model.operation.VideoCommentsRequest;
import com.acme.tubeSurf.model.output.CommentsAnalyzeSummary;
import com.acme.tubeSurf.services.CommentsChunkHandler;
import com.acme.tubeSurf.services.CommentsJobProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentThreadController {

//    @Autowired
//    private CommentThreadService commentThreadService;
    @Autowired
    private CommentsChunkHandler commentsChunkHandler;
    @Autowired
    private CommentsJobProducer commentsJobProducer;

//    @GetMapping("/commentThreadTest/{videoId}")
//    public void callCommentThreadApi(@RequestParam("videoId") String videoId, @RequestParam("commentsLimit") Integer commentsLimit) {
//        System.out.println("comment thread test for videoId = " + videoId);
//        commentThreadService.callCommentThreadApi(videoId, commentsLimit, null);
//    }
//
//    @GetMapping("/commentThread/{videoId}")
//    public String handleCommentThreadForVideo(@RequestParam("videoId") String videoId, @RequestParam("commentCount") int commentCount) {
//        UUID jobUid = UUID.randomUUID();
//        System.out.println("handle comments for videoId = " + videoId + " count: " + commentCount);
//        commentThreadService.manageCommentThreads(jobUid.toString(), videoId, commentCount);
//        return jobUid.toString();
//    }

    @PostMapping("/commentThread/bq")
    public String handleCommentThreadForVideoWithQueue(@RequestBody VideoCommentsRequest videoCommentsRequest) {
        UUID jobUid = UUID.randomUUID();
        videoCommentsRequest.setJobId(jobUid.toString());
        System.out.println("Received Video Comments Req for videoId = " + videoCommentsRequest.getVideoId() + " count: " + videoCommentsRequest.getTotalCommentsRequired() + " jobId: " + videoCommentsRequest.getJobId());
        commentsJobProducer.initVideoCommentsJob(videoCommentsRequest);
        return jobUid.toString();
    }

    @PostMapping("/syncChunkHandle")
    public String handleCommentChunkSync(@RequestBody VideoCommentsRequest videoCommentsRequest) {
        UUID jobUid = UUID.randomUUID();
        videoCommentsRequest.setJobId(jobUid.toString());
        CommentChunkRequest commentChunkRequest = new CommentChunkRequest(
                videoCommentsRequest.getJobId(),
                videoCommentsRequest.getVideoId(),
                null,
                videoCommentsRequest.getCommentsInPage(),
                videoCommentsRequest.getTotalCommentsRequired(),
                0);

        int commentsSyncCount = commentsChunkHandler.readAndSaveCommentChunkSync(commentChunkRequest);

        return "Sync Comments Count: " + commentsSyncCount + " for jobId: " + jobUid.toString();
    }

    @GetMapping("/getVideoCommentsSummary/{videoId}")
    public CommentsAnalyzeSummary getVideoCommentsSummary(@PathVariable("videoId") String videoId){
        System.out.println("getVideoCommentsSummary for videoId = " + videoId);
        return commentsChunkHandler.singleChunkAnalyze(videoId, 50);
    }

    @GetMapping("/getVideoCommentsSummaryByJobId/{jobId}")
    public CommentsAnalyzeSummary getVideoCommentsSummaryByJobId(@PathVariable("jobId") String jobId){
        System.out.println("getVideoCommentsSummary for jobId = " + jobId);
        return commentsChunkHandler.getCommentsSummaryForJobId(jobId);
    }

}
