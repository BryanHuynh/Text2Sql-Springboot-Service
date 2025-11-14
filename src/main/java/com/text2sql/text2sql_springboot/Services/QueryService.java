package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.text2sql.text2sql_springboot.DTO.*;
import com.text2sql.text2sql_springboot.Entities.*;
import com.text2sql.text2sql_springboot.Repositories.PendingJobsRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class QueryService {
    private final PendingJobsRepository pendingJobsRepository;
    private final MLServiceClient mlServiceClient;
    private final SchemaModelConstructionService smConstructionService;
    private final QueryConstructionService querySchemaConstructionService;
    private final UserDatabaseRepository userDatabaseRepository;

    public QueryService(
            PendingJobsRepository pendingJobsRepository,
            MLServiceClient mlServiceClient,
            SchemaModelConstructionService schemaModelConstructionService,
            QueryConstructionService querySchemaConstructionService,
            UserDatabaseRepository userDatabaseRepository
    ) {
        this.pendingJobsRepository = pendingJobsRepository;
        this.mlServiceClient = mlServiceClient;
        this.smConstructionService = schemaModelConstructionService;
        this.querySchemaConstructionService = querySchemaConstructionService;
        this.userDatabaseRepository = userDatabaseRepository;
    }


    public void query(QueryRequest request) throws JsonProcessingException {
        if (!ping().getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                              "Upstream Server unreachable. Please try again later");
        }
        ;
        Optional<UserDatabase> db = userDatabaseRepository.findById(request.getDatabase_id());
        if (db.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database not found");
        }
        SchemaModel schema = smConstructionService.constructSchema(db.get());
        QuerySchemaRequest payloadRequest = querySchemaConstructionService.constructHttpRequest(
                request,
                schema);

        ResponseEntity<MLQueueResponse> response = mlServiceClient.queueJob(payloadRequest);

        if (response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null
                && response.getBody().ok()
                && !response.getBody().status().equals(MLQueueStatusResponses.error)
        ) {
            MLQueueResponse body = response.getBody();
            if (body.status().equals(MLQueueStatusResponses.queued)) {
                pendingJobsRepository.save(
                        new PendingJobs(
                                payloadRequest.getId(),
                                db.get().getUser(),
                                PendingJobs.JobStatus.STARTED)
                );
            }
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "There was an error upstream");
        }
    }

    public ResponseEntity<MLPingResponse> ping() {
        return mlServiceClient.ping();
    }


}
