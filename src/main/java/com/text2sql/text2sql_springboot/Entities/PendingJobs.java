package com.text2sql.text2sql_springboot.Entities;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "PENDING_JOBS", schema = "public")
public class PendingJobs {
    @Id
    @Column(name = "CORRELATION_ID")
    private UUID correlationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user"))
    private UserDetail userDetail;

    public enum JobStatus {
        STARTED, SUCCESS, FAILED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, columnDefinition = "JOBSTATUS")
    private JobStatus jobStatus;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private OffsetDateTime updatedDate;

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public UserDetail getUserDetail() {
        return userDetail;
    }

    public void setUserDetail(UserDetail userDetails) {
        this.userDetail = userDetails;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public OffsetDateTime getUpdatedDate() {
        return updatedDate;
    }

    public PendingJobs(UUID correlationId, UserDetail userDetail, JobStatus jobStatus) {
        this.setCorrelationId(correlationId);
        this.setUserDetail(userDetail);
        this.setJobStatus(jobStatus);
    }
}
