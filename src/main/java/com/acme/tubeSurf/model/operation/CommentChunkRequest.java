package com.acme.tubeSurf.model.operation;

public class CommentChunkRequest {
    private String jobId;
    private String videoId;
    private String nextPageToken;
    private int commentsInPage;
    private int totalCommentsRequired;
    private int commentsCurrentCount;


    public CommentChunkRequest(String jobId, String videoId, String nextPageToken, int commentsInPage, int totalCommentsRequired, int commentsCurrentCount) {
        this.jobId = jobId;
        this.videoId = videoId;
        this.nextPageToken = nextPageToken;
        this.totalCommentsRequired = totalCommentsRequired;
        this.commentsInPage = commentsInPage;
        this.commentsCurrentCount = commentsCurrentCount;
    }

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

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public int getTotalCommentsRequired() {
        return totalCommentsRequired;
    }

    public void setTotalCommentsRequired(int totalCommentsRequired) {
        this.totalCommentsRequired = totalCommentsRequired;
    }
    public int getCommentsInPage() {
        return commentsInPage;
    }
    public void setCommentsInPage(int commentsInPage) {
        this.commentsInPage = commentsInPage;
    }

    public int getCommentsCurrentCount() {
        return commentsCurrentCount;
    }

    public void setCommentsCurrentCount(int commentsCurrentCount) {
        this.commentsCurrentCount = commentsCurrentCount;
    }
}
