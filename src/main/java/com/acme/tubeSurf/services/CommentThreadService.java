package com.acme.tubeSurf.services;

import com.acme.tubeSurf.model.rawComment.Comment;
import com.acme.tubeSurf.model.rawComment.CommentThread;
import com.acme.tubeSurf.model.output.ConciseComment;
import com.acme.tubeSurf.repositories.ConciseCommentRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentThreadService {
//    @Value("${youtube.api.key}")
//    private String API_KEY;
//    @Autowired
//    private ConciseCommentRepository conciseCommentRepository;
//    private static final String COMMENT_THREADS_BASE_API = "https://youtube.googleapis.com/youtube/v3/commentThreads";
//    private static final int MAX_RESULTS = 50;
//
//    Logger logger = LoggerFactory.getLogger(CommentThreadService.class);
//
//    public CommentThread callCommentThreadApi(String videoId, Integer requestedCommentsCount, @Nullable String nextPageToken) {
//
//        int commentsLimit = requestedCommentsCount == null ? MAX_RESULTS : requestedCommentsCount;
//        String commentThreadsUri = COMMENT_THREADS_BASE_API + "?part=snippet&videoId="+ videoId + "&key=" + API_KEY + "&maxResults=" + commentsLimit;
//        if (nextPageToken != null) {
//            commentThreadsUri += "&pageToken=" + nextPageToken;
//        }
//
//        // get comment threads for a video
//        try {
//            URL url = new URL(commentThreadsUri);
//            logger.info("comment threads url: " + url);
//
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//
//            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
//            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//            CommentThread commentThread = objectMapper.readValue(connection.getInputStream(), CommentThread.class);
//
//            return commentThread;
//
//        } catch (IOException e) {
//            logger.error("Error getting comment threads for videoId: " + videoId, e);
//            return null;
//        }
//    }
//
//    @Async
//    public void manageCommentThreads(String jobId, String videoId, int requestedCommentsCount) {
//        int receivedComments = 0;
//        boolean keepRunning = true;
//        String nextPageToken = null;
//
//        while (keepRunning) {
//            CommentThread commentThread = callCommentThreadApi(videoId, requestedCommentsCount, nextPageToken);
//            if (commentThread == null) {
//                break;
//            }
//
//            if (commentThread.getNextPageToken() != null) {
//                nextPageToken = commentThread.getNextPageToken();
//            }
//
//            List<Comment> rawComments = commentThread.getComments();
//            convertAndSaveComments(rawComments, jobId);
//
//            receivedComments += rawComments.size();
//            keepRunning = receivedComments < requestedCommentsCount;
//        }
//
//    }
//
//    private void convertAndSaveComments(List<Comment> rawComments, String jobId) {
//        List<ConciseComment> conciseCommentList = new ArrayList<>();
//        for (Comment comment : rawComments) {
//            ConciseComment conciseComment = getConciseCommentFromComment(comment, jobId);
//            if (conciseComment != null) {
//                conciseCommentList.add(conciseComment);
//            }
//        }
//        conciseCommentRepository.saveAll(conciseCommentList);
//    }
//
//    private ConciseComment getConciseCommentFromComment(Comment rawComment, String jobId) {
//        if (rawComment.getOuterSnippet() == null || rawComment.getOuterSnippet().getTopLevelComment() == null){
//            return null;
//        }
//
//        ConciseComment conciseComment = new ConciseComment();
//        conciseComment.setJobId(jobId);
//        conciseComment.setTopLevelCommentId(rawComment.getId());
//        conciseComment.setChannelId(rawComment.getOuterSnippet().getChannelId());
//        conciseComment.setVideoId(rawComment.getOuterSnippet().getVideoId());
//        conciseComment.setTextDisplay(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getTextDisplay());
//        conciseComment.setTextOriginal(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getTextOriginal());
//        conciseComment.setAuthorDisplayName(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorDisplayName());
//        conciseComment.setAuthorProfileImageUrl(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorProfileImageUrl());
//        conciseComment.setCanRate(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().isCanRate());
//        conciseComment.setLikeCount(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getLikeCount());
//        conciseComment.setViewerRating(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getViewerRating());
//        conciseComment.setPublishedAt(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getPublishedAt());
//        conciseComment.setUpdatedAt(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getUpdatedAt());
//
//        return conciseComment;
//    }

}
