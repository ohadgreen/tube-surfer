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
    private static final int MAX_TOP_RATED_COMMENTS = 10;
    private static final int MAX_WORD_FREQUENCY = 10;
    @Autowired
    private BlockingQueue<CommentChunkRequest> urlQueue;
    @Autowired
    private WordCountService wordCountService;


    public CommentsAnalyzeSummary getCommentsSummaryForJobId(String jobId) {
        Optional<CommentsJobSummary> commentsJobSummaryFromDb = commentsJobSummaryRepository.findById(jobId);
        if (commentsJobSummaryFromDb.isEmpty()) {
            return new CommentsAnalyzeSummary();
        } else {
            List<ConciseComment> conciseComments = conciseCommentRepository.findConciseCommentByJobId(jobId);
            List<CommentDto> conciseCommentsDtoList = conciseComments.stream().map(this::getCommentDtoFromConciseComment).toList();

            return commentsAnalysisCalculation(jobId, commentsJobSummaryFromDb.get().getVideoId(), commentsJobSummaryFromDb.get().isCompleted(), conciseCommentsDtoList, commentsJobSummaryFromDb.get().getWordCount());
        }
    }

    public CommentsAnalyzeSummary singleChunkAnalyze(String videoId, int commentsInPage) {
        CommentThread commentThread = callCommentThreadApi(videoId, commentsInPage, null);
        if (commentThread == null) {
            return null;
        }

        List<Comment> comments = commentThread.getComments();
        List<CommentDto> conciseComments = comments.stream().map(this::getCommentDtoFromComment).toList();

        Map<String, Integer> wordCount = new HashMap<>();
        wordCountService.wordsCount(wordCount, conciseComments.stream().map(CommentDto::getText).collect(Collectors.toList()));

        return commentsAnalysisCalculation("dummy", videoId, true, conciseComments, wordCount);
    }

    private CommentsAnalyzeSummary commentsAnalysisCalculation(String jobId, String videoId, boolean isCompleted, List<CommentDto> commentsList, Map<String, Integer> wordsCountMap) {
        Comparator<CommentDto> comparator = Comparator.comparing(CommentDto::getLikeCount).reversed();
        comparator = comparator.thenComparing(CommentDto::getPublishedAt);

        List<CommentDto> topRatedComments = commentsList.stream().sorted(comparator).limit(MAX_TOP_RATED_COMMENTS).toList();


        List<Map.Entry<String, Integer>> wordList = new ArrayList<>(wordsCountMap.entrySet());
        wordList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        List<Map.Entry<String, Integer>> subList = wordList.subList(0, Math.min(MAX_WORD_FREQUENCY, wordList.size()));

        LinkedHashMap<String, Integer> sortedWordsList = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : subList) {
            sortedWordsList.put(entry.getKey(), entry.getValue());
        }
        return new CommentsAnalyzeSummary(jobId, videoId, isCompleted, commentsList.size(), sortedWordsList, topRatedComments);
    }

    public int readAndSaveCommentChunkSync(CommentChunkRequest commentChunkRequest) {
        String jobId = commentChunkRequest.getJobId();
        logger.info("read and save sync videoId: {} with jobId: {} commentsCurrentCount: {} commentsInPage: {} totalCommentsRequired: {}", commentChunkRequest.getVideoId(), jobId, commentChunkRequest.getCommentsCurrentCount(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getTotalCommentsRequired());
        CommentThread commentThread = callCommentThreadApi(commentChunkRequest.getVideoId(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getNextPageToken());

        if (commentThread == null) {
            return 0;
        }

        List<Comment> rawComments = commentThread.getComments();

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

    public void receiveCommentChunk(CommentChunkRequest commentChunkRequest) {
        String jobId = commentChunkRequest.getJobId();
        logger.info("Consuming job for videoId: {} with jobId: {} commentsCurrentCount: {} commentsInPage: {} totalCommentsRequired: {}", commentChunkRequest.getVideoId(), jobId, commentChunkRequest.getCommentsCurrentCount(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getTotalCommentsRequired());
        CommentThread commentThread = callCommentThreadApi(commentChunkRequest.getVideoId(), commentChunkRequest.getCommentsInPage(), commentChunkRequest.getNextPageToken());

        if (commentThread == null) {
            return;
        }

        List<Comment> rawComments = commentThread.getComments();
        List<ConciseComment> conciseCommentList = rawComments.stream().map(comment -> getConciseCommentFromComment(comment, jobId)).filter(Objects::nonNull).collect(Collectors.toList());
        conciseCommentRepository.saveAll(conciseCommentList);

        CommentsJobSummary commentsJobSummary = new CommentsJobSummary();

        Map<String, Integer> wordCount = new HashMap<>();
        Optional<CommentsJobSummary> commentsJobSummaryFromDb = commentsJobSummaryRepository.findById(commentChunkRequest.getJobId());

        int currentCommentCount = 0;

        if (commentsJobSummaryFromDb.isEmpty()) {
            commentsJobSummary.setJobId(commentChunkRequest.getJobId());
            commentsJobSummary.setVideoId(commentChunkRequest.getVideoId());

        } else {
            commentsJobSummary = commentsJobSummaryFromDb.get();
            currentCommentCount = commentsJobSummary.getTotalComments();
            wordCount = commentsJobSummary.getWordCount();
        }

        currentCommentCount = currentCommentCount + conciseCommentList.size();
        commentsJobSummary.setTotalComments(currentCommentCount);

        wordCountService.wordsCount(wordCount, conciseCommentList.stream().map(ConciseComment::getTextDisplay).collect(Collectors.toList()));
        commentsJobSummary.setWordCount(wordCount);

        boolean isJobCompleted = currentCommentCount >= commentChunkRequest.getTotalCommentsRequired();
        commentsJobSummary.setCompleted(isJobCompleted);

        commentsJobSummaryRepository.save(commentsJobSummary);

        if (!isJobCompleted && commentThread.getNextPageToken() != null) {
            CommentChunkRequest nextPageRequest = new CommentChunkRequest(
                    commentChunkRequest.getJobId(),
                    commentChunkRequest.getVideoId(),
                    commentThread.getNextPageToken(),
                    commentChunkRequest.getCommentsInPage(),
                    commentChunkRequest.getTotalCommentsRequired(),
                    currentCommentCount);

            urlQueue.add(nextPageRequest);
        } else {
            logger.info("Job completed for jobId: {} with videoId: {} totalCommentsRequired: {}", jobId, commentChunkRequest.getVideoId(), commentChunkRequest.getTotalCommentsRequired());
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("Error in thread sleep", e);
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

            return objectMapper.readValue(connection.getInputStream(), CommentThread.class);

        } catch (IOException e) {
            logger.error("Error getting comment threads for videoId: {}", videoId, e);
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

    private CommentDto getCommentDtoFromConciseComment(ConciseComment conciseComment) {
        return new CommentDto(
                conciseComment.getTextOriginal(),
                conciseComment.getLikeCount(),
                conciseComment.getAuthorDisplayName(),
                conciseComment.getAuthorProfileImageUrl(),
                conciseComment.getPublishedAt()
        );
    }

}
