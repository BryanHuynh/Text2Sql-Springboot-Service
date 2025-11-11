package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.DTO.QueryRequest;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mlHttpConstructionService = new MLHttpConstructionService(callbackUrlFactory);
        objectMapper = new ObjectMapper();

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
        String result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Parse the JSON to verify structure
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        assertEquals("What is the total revenue?", jsonMap.get("question"));
        assertEquals("sales_db", jsonMap.get("dbId"));
        assertNotNull(jsonMap.get("callbackUrl"));
        assertNotNull(jsonMap.get("id"));
        assertNotNull(jsonMap.get("schema"));

        // Verify callback URL format
        String callbackUrl = (String) jsonMap.get("callbackUrl");
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
        String result1 = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);
        String result2 = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> json1 = objectMapper.readValue(result1, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> json2 = objectMapper.readValue(result2, Map.class);

        String id1 = (String) json1.get("id");
        String id2 = (String) json2.get("id");

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
        String result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        assertEquals(questionWithSpecialChars, jsonMap.get("question"));
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
        String result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        String correlationId = (String) jsonMap.get("id");
        String callbackUrl = (String) jsonMap.get("callbackUrl");

        assertNotNull(correlationId);
        assertNotNull(callbackUrl);
        assertTrue(callbackUrl.contains(correlationId),
                "Callback URL should contain the correlation ID");
    }

    @Test
    @DisplayName("Should produce valid JSON that can be parsed back")
    void testConstructHttpRequest_ProducesValidJson() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        QueryRequest queryRequest = new QueryRequest("Test query", databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("test_db");
        schemaBuilder.addTable("test_table");
        schemaBuilder.addColumn("test_table", "col1", "text");
        schemaBuilder.addColumn("test_table", "col2", "integer");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        String result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then - should not throw any exception when parsing
        assertDoesNotThrow(() -> {
            objectMapper.readValue(result, Map.class);
        });

        // Verify it's properly formatted JSON
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("Should include all required fields in JSON output")
    void testConstructHttpRequest_ContainsAllRequiredFields() throws JsonProcessingException {
        // Given
        UUID databaseId = UUID.randomUUID();
        QueryRequest queryRequest = new QueryRequest("Get data", databaseId);

        SchemaModel.Builder schemaBuilder = new SchemaModel.Builder();
        schemaBuilder.setDbId("required_fields_db");
        schemaBuilder.addTable("table1");
        schemaBuilder.addColumn("table1", "id", "integer");
        SchemaModel schemaModel = schemaBuilder.build();

        // When
        String result = mlHttpConstructionService.constructHttpRequest(queryRequest, schemaModel);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        // Verify all required fields are present
        assertTrue(jsonMap.containsKey("schema"), "JSON should contain 'schema' field");
        assertTrue(jsonMap.containsKey("question"), "JSON should contain 'question' field");
        assertTrue(jsonMap.containsKey("dbId"), "JSON should contain 'dbId' field");
        assertTrue(jsonMap.containsKey("callbackUrl"), "JSON should contain 'callbackUrl' field");
        assertTrue(jsonMap.containsKey("id"), "JSON should contain 'id' field");

        // Verify no null values for required fields
        assertNotNull(jsonMap.get("schema"));
        assertNotNull(jsonMap.get("question"));
        assertNotNull(jsonMap.get("dbId"));
        assertNotNull(jsonMap.get("callbackUrl"));
        assertNotNull(jsonMap.get("id"));
    }
}