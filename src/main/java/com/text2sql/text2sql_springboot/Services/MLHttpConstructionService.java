package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.DTO.QueryRequest;
import com.text2sql.text2sql_springboot.DTO.QuerySchemaRequest;
import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class MLHttpConstructionService {
    private final QueryServiceCallbackUrlFactory callbackUrlFactory;

    public MLHttpConstructionService(QueryServiceCallbackUrlFactory callbackUrlFactory) {
        this.callbackUrlFactory = callbackUrlFactory;
    }

    public QuerySchemaRequest constructHttpRequest(QueryRequest req, SchemaModel schemaModel) throws JsonProcessingException {
        UUID correlationId = UUID.randomUUID();
        QuerySchemaRequest.Builder builder = new QuerySchemaRequest.Builder();
        Map<String, Object> schema = schemaModel.toSchemaMap();
        return builder
                .schema(schema)
                .question(req.getQuestion())
                .dbId((String) schema.get("db_id"))
                .callbackUrl(callbackUrlFactory.buildJobCallbackUrl(correlationId.toString()))
                .id(correlationId)
                .build();

    }
}
