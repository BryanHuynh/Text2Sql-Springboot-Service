package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.*;
import com.text2sql.text2sql_springboot.Entities.*;
import com.text2sql.text2sql_springboot.Repositories.PendingJobsRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class QueryService {
    private final PendingJobsRepository pendingJobsRepository;
    private final MLServiceProps mlServiceProps;
    private final SignatureService signatureService;
    private final SchemaModelConstructionService smConstructionService;
    private final QueryConstructionService querySchemaConstructionService;
    private final UserDatabaseRepository userDatabaseRepository;

    public QueryService(
            PendingJobsRepository pendingJobsRepository,
            MLServiceProps mlServiceProps,
            SignatureService signatureService,
            SchemaModelConstructionService schemaModelConstructionService,
            QueryConstructionService querySchemaConstructionService,
            UserDatabaseRepository userDatabaseRepository
    ) {
        this.pendingJobsRepository = pendingJobsRepository;
        this.mlServiceProps = mlServiceProps;
        this.signatureService = signatureService;
        this.smConstructionService = schemaModelConstructionService;
        this.querySchemaConstructionService = querySchemaConstructionService;
        this.userDatabaseRepository = userDatabaseRepository;
    }


    public void query(QueryRequest request) throws JsonProcessingException {
        if (!ping().getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Upstream Server unreachable. Please try again later");
        }
        Optional<UserDatabase> db = userDatabaseRepository.findById(request.getDatabase_id());
        if (db.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database not found");
        }
        SchemaModel schema = smConstructionService.constructSchema(db.get());
        QuerySchemaRequest payloadRequest = querySchemaConstructionService.constructHttpRequest(request, schema);

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", signatureService.generateSignature(payload));

        HttpEntity<String> httpRequest = new HttpEntity<>(payload, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<MLQueueResponse> response = restTemplate.postForEntity(
                UriComponentsBuilder.fromUriString(mlServiceProps.getUrl())
                        .path("queue")
                        .build()
                        .toUriString(),
                httpRequest,
                MLQueueResponse.class
        );
        if (response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null
                && response.getBody().ok()
                && response.getBody().status().equals(MLQueueStatusResponses.queued)) {
            pendingJobsRepository.save(
                    new PendingJobs(
                            payloadRequest.getId(),
                            db.get().getUser(),
                            PendingJobs.JobStatus.STARTED)
            );
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<MLPingResponse> ping() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForEntity(mlServiceProps.getUrl() + "/ping", MLPingResponse.class);
    }


}
