package com.acme.tubeSurf.model.output;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
@Document(collection = "video_comments_summary")
public class CommentsAnalyzeSummary {
    private String jobId;
    private String videoId;
    private int totalComments;
    private Map<String, Integer> wordsFrequency;
    private Map<Integer, String> topRatedComments;


}
