package com.acme.tubeSurf.model.operation;

public class JobIdResponse {
    private String jobId;

    public JobIdResponse(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
