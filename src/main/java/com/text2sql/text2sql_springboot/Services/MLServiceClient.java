package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.MLPingResponse;
import com.text2sql.text2sql_springboot.DTO.MLQueueResponse;
import com.text2sql.text2sql_springboot.DTO.QuerySchemaRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MLServiceClient {
    private final RestTemplate restTemplate;
    private final MLServiceProps mlServiceProps;
    private final SignatureService signatureService;
    private final ObjectMapper objectMapper;

    public MLServiceClient(
            MLServiceProps mlServiceProps,
            SignatureService signatureService
    ) {
        this.restTemplate = new RestTemplate();
        this.mlServiceProps = mlServiceProps;
        this.signatureService = signatureService;
        this.objectMapper = new ObjectMapper();
    }

    public ResponseEntity<MLPingResponse> ping() {
        return restTemplate.getForEntity(mlServiceProps.getUrl() + "/ping", MLPingResponse.class);
    }

    public ResponseEntity<MLQueueResponse> queueJob(QuerySchemaRequest payloadRequest) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(payloadRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", signatureService.generateSignature(payload));

        HttpEntity<String> httpRequest = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                UriComponentsBuilder.fromUriString(mlServiceProps.getUrl())
                        .path("queue")
                        .build()
                        .toUriString(),
                httpRequest,
                MLQueueResponse.class
        );
    }
}