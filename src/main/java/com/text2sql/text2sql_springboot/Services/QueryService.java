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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class QueryService {
    private final TableVariablesRepository tableVariablesRepository;
    private final UserDatabaseRepository userDatabaseRepository;
    private final UserTableRepository userTableRepository;
    private final PendingJobsRepository pendingJobsRepository;
    private final QueryServiceCallbackUrlFactory callbackUrlFactory;
    private final MLServiceProps mlServiceProps;
    private final SignatureService signatureService;


    public QueryService(
            TableVariablesRepository tableVariablesRepository,
            UserDatabaseRepository userDatabaseRepository,
            UserTableRepository userTableRepository,
            PendingJobsRepository pendingJobsRepository,
            MLServiceProps mlServiceProps,
            QueryServiceCallbackUrlFactory callbackUrlFactory,
            SignatureService signatureService
    ) {
        this.tableVariablesRepository = tableVariablesRepository;
        this.userDatabaseRepository = userDatabaseRepository;
        this.userTableRepository = userTableRepository;
        this.pendingJobsRepository = pendingJobsRepository;
        this.mlServiceProps = mlServiceProps;
        this.callbackUrlFactory = callbackUrlFactory;
        this.signatureService = signatureService;
    }

    public String constructHttpRequest(QueryRequest req, UserDetail user) throws JsonProcessingException {
        UUID correlationId = UUID.randomUUID();
        QuerySchemaRequest.Builder builder = new QuerySchemaRequest.Builder();
        Map<String, Object> schema = constructSchema(req.getDatabase_id()).toSchemaMap();
        QuerySchemaRequest query = builder.schema(schema)
                .question(req.getQuestion())
                .dbId((String) schema.get("db_id"))
                .callbackUrl(callbackUrlFactory.buildJobCallbackUrl(correlationId.toString()))
                .correlationId(correlationId)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(query);
    }

    public void query(String request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", signatureService.generateSignature(request));
        HttpEntity<String> httpRequest = new HttpEntity<>(request, headers);
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

    public SchemaModel constructSchema(UUID dbId) throws ResponseStatusException {
        Optional<UserDatabase> db = userDatabaseRepository.findById(dbId);
        if (db.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database not found");
        }
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.setDbId(db.get().getDatabaseName());

        List<UserTable> tables = userTableRepository.findByUserDatabase(db.get());
        if (tables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Tables Present");
        }
        for (UserTable table : tables) {
            builder.addTable(table.getTableName());
        }

        List<TableVariable> tableVariables = new ArrayList<>();
        for (UserTable table : tables) {
            List<TableVariable> variables = tableVariablesRepository.findAllByUserTable(table);
            tableVariables.addAll(variables);
        }
        if (tableVariables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Table Variables Present");
        }

        tableVariables.forEach(tableVariable -> {
            String tableName = tableVariable.getUserTable().getTableName();
            String variableName = tableVariable.getVariableName();
            String type = tableVariable.getVariableType();
            builder.addColumn(tableName, variableName, type);
            if (tableVariable.isPkFlag()) {
                builder.addPrimaryKey(tableName, variableName);
            }
        });

        tableVariables.forEach(tableVariable -> {
            if (tableVariable.isFkFlag()) {
                TableVariable primary = tableVariable.getFkRef();
                String primaryTable = primary.getUserTable().getTableName();
                String primaryVariable = primary.getVariableName();
                String fkTable = tableVariable.getUserTable().getTableName();

                builder.addForeignKey(primaryTable, primaryVariable, fkTable, tableVariable.getVariableName());
            }
        });
        return builder.build();
    }




}
