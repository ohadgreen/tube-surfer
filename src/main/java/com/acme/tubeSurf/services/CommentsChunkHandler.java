package com.acme.tubeSurf.services;

import com.acme.tubeSurf.model.operation.CommentChunkRequest;
import com.acme.tubeSurf.model.output.CommentDto;
import com.acme.tubeSurf.model.output.CommentsAnalyzeSummary;
import com.acme.tubeSurf.model.output.CommentsJobSummary;
import com.acme.tubeSurf.model.output.ConciseComment;
import com.acme.tubeSurf.model.rawComment.Comment;
import com.acme.tubeSurf.model.rawComment.CommentThread;
import com.acme.tubeSurf.repositories.CommentsJobSummaryRepository;
import com.acme.tubeSurf.repositories.ConciseCommentRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Component
public class CommentsChunkHandler {
    Logger logger = LoggerFactory.getLogger(CommentsChunkHandler.class);
    @Value("${youtube.api.key}")
    private String API_KEY;
    @Autowired
    private ConciseCommentRepository conciseCommentRepository;
    @Autowired
    private CommentsJobSummaryRepository commentsJobSummaryRepository;
    private static final String COMMENT_THREADS_BASE_API = "https://youtube.googleapis.com/youtube/v3/commentThreads";
    private static final int MAX_RESULTS = 50;
    private static final int MAX_TOP_RATED_COMMENTS = 5;
    private static final int MAX_WORD_FREQUENCY = 10;
    @Autowired
    private BlockingQueue<CommentChunkRequest> urlQueue;
    @Autowired
    private WordCountService wordCountService;


    public CommentsAnalyzeSummary singleChunkAnalyze(String videoId, int commentsInPage) {
        CommentThread commentThread = callCommentThreadApi(videoId, commentsInPage, null);
        List<Comment> comments = commentThread.getComments();
        List<CommentDto> conciseComments = comments.stream().map(comment -> getCommentDtoFromComment(comment)).toList();

        Comparator<CommentDto> comparator = Comparator.comparing(CommentDto::getLikeCount).reversed();
        comparator = comparator.thenComparing(CommentDto::getPublishedAt);

        List<CommentDto> topRatedComments = conciseComments.stream().sorted(comparator).limit(MAX_TOP_RATED_COMMENTS).toList();

        Map<String, Integer> wordCount = new HashMap<>();
        wordCountService.wordsCount(wordCount, conciseComments.stream().map(CommentDto::getText).collect(Collectors.toList()));

        List<Map.Entry<String, Integer>> wordList = new ArrayList<>(wordCount.entrySet());
        wordList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        List<Map.Entry<String, Integer>> subList = wordList.subList(0, Math.min(MAX_WORD_FREQUENCY, wordList.size()));

        LinkedHashMap<String, Integer> sortedWordsList = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : subList) {
            sortedWordsList.put(entry.getKey(), entry.getValue());
        }
        return new CommentsAnalyzeSummary("dummy", videoId, conciseComments.size(), sortedWordsList, topRatedComments);
    }

    public int readAndSaveCommentChunkSync(CommentChunkRequest commentChunkRequest) {
        String jobId = commentChunkRequest.getJobId();
        logger.info("read and save sync videoId: {} with jobId: {} commentsCurrentCount: {} commentsInPage: {} totalCommentsRequired: {}", commentChunkRequest.getVideoId(), jobId, commentChunkRequest.getCommentsCurrentCount(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getTotalCommentsRequired());
        CommentThread commentThread = callCommentThreadApi(commentChunkRequest.getVideoId(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getNextPageToken());

        if (commentThread == null) {
            return 0;
        }

        List<Comment> rawComments = commentThread.getComments();

//        convertAndSaveComments(rawComments, commentChunkRequest.getJobId());
        List<ConciseComment> conciseCommentList = rawComments.stream().map(comment -> getConciseCommentFromComment(comment, jobId)).filter(Objects::nonNull).collect(Collectors.toList());
        conciseCommentRepository.saveAll(conciseCommentList);

        Map<String, Integer> wordCount = new HashMap<>();
        wordCountService.wordsCount(wordCount, conciseCommentList.stream().map(ConciseComment::getTextDisplay).collect(Collectors.toList()));

        CommentsJobSummary commentsJobSummary = new CommentsJobSummary();
        commentsJobSummary.setJobId(jobId);
        commentsJobSummary.setVideoId(commentChunkRequest.getVideoId());
        commentsJobSummary.setTotalComments(conciseCommentList.size());
        commentsJobSummary.setWordCount(wordCount);

        commentsJobSummaryRepository.save(commentsJobSummary);

        return rawComments.size();
    }

//    public

    public void receiveCommentChunk(CommentChunkRequest commentChunkRequest) {
        String jobId = commentChunkRequest.getJobId();
        logger.info("Consuming job for videoId: " + commentChunkRequest.getVideoId() + " with jobId: " + jobId + " commentsCurrentCount: " + commentChunkRequest.getCommentsCurrentCount() + " commentsInPage: " + commentChunkRequest.getCommentsInPage() + " totalCommentsRequired: " + commentChunkRequest.getTotalCommentsRequired());
        CommentThread commentThread = callCommentThreadApi(commentChunkRequest.getVideoId(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getNextPageToken());

        if (commentThread == null) {
            return;
        }

        List<Comment> rawComments = commentThread.getComments();

//        convertAndSaveComments(rawComments, commentChunkRequest.getJobId());
        List<ConciseComment> conciseCommentList = rawComments.stream().map(comment -> getConciseCommentFromComment(comment, jobId)).filter(Objects::nonNull).collect(Collectors.toList());
        conciseCommentRepository.saveAll(conciseCommentList);

        CommentsJobSummary commentsJobSummary = commentsJobSummaryRepository.findCommentsJobSummaryByJobId(commentChunkRequest.getJobId());
        if (commentsJobSummary == null) {
            commentsJobSummary = new CommentsJobSummary();
            commentsJobSummary.setJobId(commentChunkRequest.getJobId());
            commentsJobSummary.setVideoId(commentChunkRequest.getVideoId());
            commentsJobSummary.setTotalComments(0);
            commentsJobSummary.setWordCount(new HashMap<>());
            commentsJobSummary.setTopRatedComments(new HashMap<>());
        }

        Map<String, Integer> wordCount = commentsJobSummary.getWordCount();

        wordCountService.wordsCount(wordCount, conciseCommentList.stream().map(ConciseComment::getTextDisplay).collect(Collectors.toList()));
        commentsJobSummary.setWordCount(wordCount);
        commentsJobSummaryRepository.save(commentsJobSummary);


        if (commentThread.getNextPageToken() != null) {
            if (commentChunkRequest.getCommentsCurrentCount() + commentChunkRequest.getCommentsInPage() >= commentChunkRequest.getTotalCommentsRequired()) {
                return;
            }
            CommentChunkRequest nextPageRequest = new CommentChunkRequest(
                    commentChunkRequest.getJobId(),
                    commentChunkRequest.getVideoId(),
                    commentThread.getNextPageToken(),
                    commentChunkRequest.getCommentsInPage(),
                    commentChunkRequest.getTotalCommentsRequired(),
                    commentChunkRequest.getCommentsCurrentCount() + commentChunkRequest.getCommentsInPage());

            urlQueue.add(nextPageRequest);
        }

        try {
            Thread.sleep(5000); // fake delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private CommentThread callCommentThreadApi(String videoId, Integer commentsInPage, @Nullable String nextPageToken) {

        int commentsLimit = commentsInPage == null ? MAX_RESULTS : commentsInPage;
        String commentThreadsUri = COMMENT_THREADS_BASE_API + "?part=snippet&videoId="+ videoId + "&key=" + API_KEY + "&maxResults=" + commentsLimit;
        if (nextPageToken != null) {
            commentThreadsUri += "&pageToken=" + nextPageToken;
        }

        // get comment threads for a video
        try {
            URL url = new URL(commentThreadsUri);
            logger.info("comment threads url: {}", url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            CommentThread commentThread = objectMapper.readValue(connection.getInputStream(), CommentThread.class);

            return commentThread;

        } catch (IOException e) {
            logger.error("Error getting comment threads for videoId: " + videoId, e);
            return null;
        }
    }


    private ConciseComment getConciseCommentFromComment(Comment rawComment, String jobId) {
        if (rawComment.getOuterSnippet() == null || rawComment.getOuterSnippet().getTopLevelComment() == null) {
            return null;
        }

        ConciseComment conciseComment = new ConciseComment();
        conciseComment.setJobId(jobId);
        conciseComment.setTopLevelCommentId(rawComment.getId());
        conciseComment.setChannelId(rawComment.getOuterSnippet().getChannelId());
        conciseComment.setVideoId(rawComment.getOuterSnippet().getVideoId());
        conciseComment.setTextDisplay(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getTextDisplay());
        conciseComment.setTextOriginal(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getTextOriginal());
        conciseComment.setAuthorDisplayName(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorDisplayName());
        conciseComment.setAuthorProfileImageUrl(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorProfileImageUrl());
        conciseComment.setCanRate(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().isCanRate());
        conciseComment.setLikeCount(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getLikeCount());
        conciseComment.setViewerRating(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getViewerRating());
        conciseComment.setPublishedAt(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getPublishedAt());
        conciseComment.setUpdatedAt(rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getUpdatedAt());

        return conciseComment;
    }

    private CommentDto getCommentDtoFromComment(Comment rawComment) {
        if (rawComment.getOuterSnippet() == null || rawComment.getOuterSnippet().getTopLevelComment() == null) {
            return null;
        }
        return new CommentDto(
                rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getTextOriginal(),
                rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getLikeCount(),
                rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorDisplayName(),
                rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getAuthorProfileImageUrl(),
                rawComment.getOuterSnippet().getTopLevelComment().getInnerSnippet().getPublishedAt()
        );
    }

}
