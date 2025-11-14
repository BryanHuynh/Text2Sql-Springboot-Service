package com.text2sql.text2sql_springboot.Entities;

import com.text2sql.text2sql_springboot.DTO.JobStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "PENDING_JOBS", schema = "public")
@EntityListeners(AuditingEntityListener.class)
public class PendingJobs {
    @Id
    @Column(name = "CORRELATION_ID")
    private UUID correlationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user"))
    private UserDetail userDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false)
    private JobStatus jobStatus;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

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

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public PendingJobs(UUID correlationId, UserDetail userDetail, JobStatus jobStatus) {
        this.setCorrelationId(correlationId);
        this.setUserDetail(userDetail);
        this.setJobStatus(jobStatus);
    }

    public PendingJobs() {

    }
}
