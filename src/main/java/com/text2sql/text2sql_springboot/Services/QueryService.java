package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.*;
import com.text2sql.text2sql_springboot.Entities.*;
import com.text2sql.text2sql_springboot.Repositories.PendingJobsRepository;
import com.text2sql.text2sql_springboot.Repositories.TableVariablesRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
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
    private final MLHttpConstructionService httpConstructionService;

    public QueryService(
            PendingJobsRepository pendingJobsRepository,
            MLServiceProps mlServiceProps,
            SignatureService signatureService,
            SchemaModelConstructionService schemaModelConstructionService,
            MLHttpConstructionService mlHttpConstructionService
    ) {
        this.pendingJobsRepository = pendingJobsRepository;
        this.mlServiceProps = mlServiceProps;
        this.signatureService = signatureService;
        this.smConstructionService = schemaModelConstructionService;
        this.httpConstructionService = mlHttpConstructionService;
    }


    public void query(QueryRequest request) throws JsonProcessingException {
        SchemaModel schema = smConstructionService.constructSchema(request.getDatabase_id());
        QuerySchemaRequest payloadRequest = httpConstructionService.constructHttpRequest(request, schema);

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", signatureService.generateSignature(payload));

        HttpEntity<String> httpRequest = new HttpEntity<>(payload, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<MLQueueStatusResponses> response = restTemplate.postForEntity(
                UriComponentsBuilder.fromUriString(mlServiceProps.getUrl())
                        .path("queue")
                        .build()
                        .toUriString(),
                httpRequest,
                MLQueueStatusResponses.class
        );
    }

    public ResponseEntity<MLPingDto> ping() {
        System.out.println("Pinging endpoint: " + mlServiceProps.getUrl());
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForEntity(mlServiceProps.getUrl() + "/ping", MLPingDto.class);
    }


}
