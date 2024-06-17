package com.acme.tubeSurf.model.output;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "comments_job_summary")
public class CommentsJobSummary {
    @Id
    private String jobId;
    private String videoId;
    private int totalComments;
    private Map<String, Integer> wordCount;
    private Map<Integer, String> topRatedComments;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(int totalComments) {
        this.totalComments = totalComments;
    }

    public Map<String, Integer> getWordCount() {
        return wordCount;
    }

    public void setWordCount(Map<String, Integer> wordCount) {
        this.wordCount = wordCount;
    }

    public Map<Integer, String> getTopRatedComments() {
        return topRatedComments;
    }

    public void setTopRatedComments(Map<Integer, String> topRatedComments) {
        this.topRatedComments = topRatedComments;
    }
}
