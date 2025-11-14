package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.DTO.MLCallbackResponse;
import com.text2sql.text2sql_springboot.Entities.PendingJobs;
import com.text2sql.text2sql_springboot.Repositories.PendingJobsRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class MLCallbackService {
    private SignatureService signatureService;
    private PendingJobsRepository pendingJobsRepository;
    private ObjectMapper objectMapper;

    public MLCallbackService(SignatureService signatureService,
                             PendingJobsRepository pendingJobsRepository) {
        this.signatureService = signatureService;
        this.pendingJobsRepository = pendingJobsRepository;
        this.objectMapper = new ObjectMapper();
    }

    public void updateJobStatus(UUID jobId,
                                MLCallbackResponse body,
                                String headerSignature) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(body);
        if (!signatureService.verifySignature(payload, headerSignature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                              "Signature verification failed");
        }
        PendingJobs job = pendingJobsRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Job not found"));
        job.setJobStatus(body.status());
        pendingJobsRepository.save(job);


    }

}
