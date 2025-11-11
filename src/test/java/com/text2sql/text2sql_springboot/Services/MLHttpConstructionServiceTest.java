package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.DTO.QueryRequest;
import com.text2sql.text2sql_springboot.DTO.QuerySchemaRequest;
import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MLHttpConstructionServiceTest {

    @Mock
    private QueryServiceCallbackUrlFactory callbackUrlFactory;

    private MLHttpConstructionService mlHttpConstructionService;

    @BeforeEach
    void setUp() {
        mlHttpConstructionService = new MLHttpConstructionService(callbackUrlFactory);
        // Mock the callback URL factory to return predictable URLs
        when(callbackUrlFactory.buildJobCallbackUrl(anyString()))
                .thenAnswer(invocation -> "http://localhost:8080/query/jobs/" + invocation.getArgument(0) + "/callback");
    }

    @Test
    @DisplayName("Should construct valid HTTP request JSON with simple schema")
    void testConstructHttpRequest_WithSimpleSchema() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        QueryRequest queryRequest = new QueryRequest("What is the total revenue?", databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("sales_db");
        schemaBuilder.addTable("customers");
        schemaBuilder.addColumn("customers", "id", "integer");
        schemaBuilder.addColumn("customers", "name", "text");
        schemaBuilder.addPrimaryKey("customers", "id");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        QuerySchemaRequest result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        assertNotNull(result);

        assertEquals("What is the total revenue?", result.getQuestion());
        assertEquals("sales_db", result.getDbId());
        assertNotNull(result.getCallbackUrl());
        assertNotNull(result.getId());
        assertNotNull(result.getSchema());

        // Verify callback URL format
        String callbackUrl = result.getCallbackUrl();
        assertTrue(callbackUrl.contains("/query/jobs/"));
        assertTrue(callbackUrl.endsWith("/callback"));
    }


    @Test
    @DisplayName("Should generate unique correlation ID for each request")
    void testConstructHttpRequest_GeneratesUniqueCorrelationId() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        QueryRequest queryRequest = new QueryRequest("Test question", databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("test_db");
        schemaBuilder.addTable("test_table");
        schemaBuilder.addColumn("test_table", "id", "integer");
        schemaBuilder.addPrimaryKey("test_table", "id");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        QuerySchemaRequest result1 = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);
        QuerySchemaRequest result2 = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        String id1 = result1.getId();
        String id2 = result2.getId();

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2, "Correlation IDs should be unique for each request");

        // Verify IDs are valid UUIDs
        assertDoesNotThrow(() -> UUID.fromString(id1));
        assertDoesNotThrow(() -> UUID.fromString(id2));
    }

    @Test
    @DisplayName("Should properly handle questions with special characters")
    void testConstructHttpRequest_WithSpecialCharacters() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        String questionWithSpecialChars = "What's the customer's \"preferred\" product? (Top 10)";
        QueryRequest queryRequest = new QueryRequest(questionWithSpecialChars, databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("test_db");
        schemaBuilder.addTable("products");
        schemaBuilder.addColumn("products", "id", "integer");
        schemaBuilder.addPrimaryKey("products", "id");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        QuerySchemaRequest result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        assertNotNull(result);

        assertEquals(questionWithSpecialChars, result.getQuestion());
    }

    @Test
    @DisplayName("Should include correlation ID in callback URL")
    void testConstructHttpRequest_CorrelationIdInCallbackUrl() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        QueryRequest queryRequest = new QueryRequest("Test question", databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("test_db");
        schemaBuilder.addTable("test_table");
        schemaBuilder.addColumn("test_table", "id", "integer");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        QuerySchemaRequest result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        String correlationId = result.getId();
        String callbackUrl = result.getCallbackUrl();

        assertNotNull(correlationId);
        assertNotNull(callbackUrl);
        assertTrue(callbackUrl.contains(correlationId),
                "Callback URL should contain the correlation ID");
    }

}