package com.text2sql.text2sql_springboot.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.text2sql.text2sql_springboot.DTO.MLCallbackResponse;
import com.text2sql.text2sql_springboot.DTO.QueryRequest;
import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import com.text2sql.text2sql_springboot.Services.MLCallbackService;
import com.text2sql.text2sql_springboot.Services.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    private final QueryService queryService;
    private final MLCallbackService mlCallbackService;

    public QueryController(QueryService queryService, MLCallbackService mlCallbackService) {
        this.queryService = queryService;
        this.mlCallbackService = mlCallbackService;
    }

    @PostMapping()
    public ResponseEntity<String> query(
            @Valid
            @RequestBody
            QueryRequest req) {
        try {
            queryService.query(req);
            return new ResponseEntity<>("Success", HttpStatus.OK);
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>("Failed to process request",
                                        HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("jobs/{id}/callback")
    public ResponseEntity<String> updateJobDetails(
            @Valid
            @PathVariable("id")
            UUID jobId,
            @Valid
            @RequestBody
            MLCallbackResponse body,
            @RequestHeader HttpHeaders headers) {
        if (!Objects.equals(headers.getContentType(), MediaType.APPLICATION_JSON)) {
            return new ResponseEntity<>("Invalid Content Type", HttpStatus.BAD_REQUEST);
        }
        String headerSignature = headers.getFirst("X-Webhook-Signature");
        if (headerSignature == null || headerSignature.isBlank()) {
            return ResponseEntity.badRequest().body("Missing or empty X-Webhook-Signature");
        }
        if (jobId != body.jobId()) {
            return ResponseEntity.badRequest().body("Invalid Job ID");
        }
        try {
            mlCallbackService.updateJobStatus(jobId, body, headerSignature);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}
