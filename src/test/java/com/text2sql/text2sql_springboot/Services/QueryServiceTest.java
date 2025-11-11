package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.*;
import com.text2sql.text2sql_springboot.Entities.PendingJobs;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Repositories.PendingJobsRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private PendingJobsRepository pendingJobsRepository;

    @Mock
    private MLServiceProps mlServiceProps;

    @Mock
    private SignatureService signatureService;

    @Mock
    private SchemaModelConstructionService smConstructionService;

    @Mock
    private QueryConstructionService httpConstructionService;

    @Mock
    private UserDatabaseRepository userDatabaseRepository;

    private QueryService queryService;

    private UUID testDatabaseId;
    private UUID testCorrelationId;
    private UserDatabase testUserDatabase;
    private UserDetail testUserDetail;
    private QueryRequest testQueryRequest;
    private SchemaModel testSchemaModel;
    private QuerySchemaRequest testQuerySchemaRequest;

    @BeforeEach
    void setUp() {
        queryService = new QueryService(
                pendingJobsRepository,
                mlServiceProps,
                signatureService,
                smConstructionService,
                httpConstructionService,
                userDatabaseRepository
        );

        // Initialize test data
        testDatabaseId = UUID.randomUUID();
        testCorrelationId = UUID.randomUUID();

        testUserDetail = new UserDetail();
        testUserDatabase = new UserDatabase("test_db", testUserDetail);

        testQueryRequest = new QueryRequest("What is the total sales?", testDatabaseId);

        testSchemaModel = new SchemaModel.Builder()
                .addTable("sales")
                .addColumn("sales", "id", "int")
                .addColumn("sales", "amount", "decimal")
                .build();

        testQuerySchemaRequest = new QuerySchemaRequest.Builder()
                .id(testCorrelationId)
                .question("What is the total sales?")
                .dbId(testDatabaseId.toString())
                .schema(testSchemaModel.toSchemaMap())
                .callbackUrl("http://callback.url")
                .build();

        when(mlServiceProps.getUrl()).thenReturn("http://ml-service.test");
    }

    @Test
    void query_shouldThrowException_whenDatabaseNotFound() {
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.empty());

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest)
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Database not found", exception.getReason());

            verify(userDatabaseRepository).findById(testDatabaseId);
            verifyNoInteractions(smConstructionService, httpConstructionService, signatureService, pendingJobsRepository);
        }
    }

    @Test
    void query_shouldThrowException_whenPingFails() {
        // Arrange
        MLPingResponse pingResponse = new MLPingResponse(false, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.SERVICE_UNAVAILABLE);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest)
            );

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());

            // Verify that no other services were called since ping failed early
            verifyNoInteractions(userDatabaseRepository, smConstructionService, httpConstructionService, signatureService, pendingJobsRepository);
        }
    }

    @Test
    void query_shouldSuccessfullyQueueJob_whenAllConditionsAreMet() throws JsonProcessingException {
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(signatureService.generateSignature(anyString())).thenReturn("test-signature");

        MLQueueResponse successResponse = new MLQueueResponse(true, MLQueueStatusResponses.queued, "Job queued successfully");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        when(pendingJobsRepository.save(any(PendingJobs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class))
                    ).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            // Act
            queryService.query(testQueryRequest);

            // Assert
            verify(userDatabaseRepository).findById(testDatabaseId);
            verify(smConstructionService).constructSchema(testUserDatabase);
            verify(httpConstructionService).constructHttpRequest(testQueryRequest, testSchemaModel);
            verify(signatureService).generateSignature(anyString());
            assertEquals(2, mockedConstruction.constructed().size());
            RestTemplate postRestTemplate = mockedConstruction.constructed().get(1);
            RestTemplate pingRestTemplate = mockedConstruction.constructed().get(0);
            verify(postRestTemplate).postForEntity(
                    contains("http://ml-service.test"),
                    any(HttpEntity.class),
                    eq(MLQueueResponse.class)
            );
            verify(pingRestTemplate).getForEntity(
                    contains("http://ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );
            verify(pendingJobsRepository).save(argThat(pendingJob ->
                    pendingJob.getCorrelationId().equals(testCorrelationId) &&
                            pendingJob.getUserDetail().equals(testUserDetail) &&
                            pendingJob.getJobStatus() == PendingJobs.JobStatus.STARTED
            ));
            verify(pendingJobsRepository, times(1))
                    .save(any(PendingJobs.class));
        }
    }

    @Test
    void query_shouldNotSavePendingJob_whenResponseIsNotSuccessful() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(signatureService.generateSignature(anyString())).thenReturn("test-signature");

        MLQueueResponse failureResponse = new MLQueueResponse(true, MLQueueStatusResponses.queued, "Job queued");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(failureResponse, HttpStatus.INTERNAL_SERVER_ERROR);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class)
                    )).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals(2, mockedConstruction.constructed().size());
            verify(userDatabaseRepository).findById(testDatabaseId);
            verify(smConstructionService).constructSchema(testUserDatabase);
            verify(httpConstructionService).constructHttpRequest(testQueryRequest, testSchemaModel);
            verify(signatureService).generateSignature(anyString());
            assertEquals(2, mockedConstruction.constructed().size());
            RestTemplate postRestTemplate = mockedConstruction.constructed().get(1);
            RestTemplate pingRestTemplate = mockedConstruction.constructed().get(0);
            verify(postRestTemplate).postForEntity(
                    contains("http://ml-service.test"),
                    any(HttpEntity.class),
                    eq(MLQueueResponse.class)
            );
            verify(pingRestTemplate).getForEntity(
                    contains("http://ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );
            verifyNoInteractions(pendingJobsRepository);

        }
    }

    @Test
    void query_shouldNotSavePendingJob_whenResponseBodyIsNull() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(signatureService.generateSignature(anyString())).thenReturn("test-signature");

        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class)
                    )).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals(2, mockedConstruction.constructed().size());
            RestTemplate postRestTemplate = mockedConstruction.constructed().get(1);
            RestTemplate pingRestTemplate = mockedConstruction.constructed().get(0);
            verify(postRestTemplate).postForEntity(
                    contains("http://ml-service.test"),
                    any(HttpEntity.class),
                    eq(MLQueueResponse.class)
            );
            verify(pingRestTemplate).getForEntity(
                    contains("http://ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );

            verify(userDatabaseRepository).findById(testDatabaseId);
            verifyNoInteractions(pendingJobsRepository);
        }
    }

    @Test
    void query_shouldNotSavePendingJob_whenOkIsFalse() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(signatureService.generateSignature(anyString())).thenReturn("test-signature");

        MLQueueResponse failureResponse = new MLQueueResponse(false, MLQueueStatusResponses.queued, "Error occurred");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(failureResponse, HttpStatus.OK);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class)
                    )).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals(2, mockedConstruction.constructed().size());
            RestTemplate postRestTemplate = mockedConstruction.constructed().get(1);
            RestTemplate pingRestTemplate = mockedConstruction.constructed().get(0);
            verify(postRestTemplate).postForEntity(
                    contains("http://ml-service.test"),
                    any(HttpEntity.class),
                    eq(MLQueueResponse.class)
            );
            verify(pingRestTemplate).getForEntity(
                    contains("http://ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );

            verify(userDatabaseRepository).findById(testDatabaseId);
            verifyNoInteractions(pendingJobsRepository);
        }
    }

    @Test
    void query_shouldNotSavePendingJob_whenStatusIsNotQueued() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(signatureService.generateSignature(anyString())).thenReturn("test-signature");

        MLQueueResponse processingResponse = new MLQueueResponse(true, MLQueueStatusResponses.processing, "Already processing");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(processingResponse, HttpStatus.OK);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class)
                    )).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> queryService.query(testQueryRequest));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals(2, mockedConstruction.constructed().size());
            RestTemplate postRestTemplate = mockedConstruction.constructed().get(1);
            RestTemplate pingRestTemplate = mockedConstruction.constructed().get(0);
            verify(postRestTemplate).postForEntity(
                    contains("http://ml-service.test"),
                    any(HttpEntity.class),
                    eq(MLQueueResponse.class)
            );
            verify(pingRestTemplate).getForEntity(
                    contains("http://ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );

            verify(userDatabaseRepository).findById(testDatabaseId);
            verifyNoInteractions(pendingJobsRepository);
        }
    }

    @Test
    void query_shouldIncludeCorrectHeadersInRequest() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);

        String expectedSignature = "test-signature-12345";
        when(signatureService.generateSignature(anyString())).thenReturn(expectedSignature);

        MLQueueResponse successResponse = new MLQueueResponse(true, MLQueueStatusResponses.queued, "Queued");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.postForEntity(
                            anyString(),
                            any(HttpEntity.class),
                            eq(MLQueueResponse.class)
                    )).thenReturn(responseEntity);
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class))
                    ).thenReturn(pingResponseEntity);
                })) {

            // Act
            queryService.query(testQueryRequest);

            // Assert
            RestTemplate mockRestTemplate = mockedConstruction.constructed().get(1);
            verify(mockRestTemplate).postForEntity(
                    anyString(),
                    argThat((HttpEntity<String> entity) -> {
                        String contentType = Objects.requireNonNull(entity.getHeaders().getContentType()).toString();
                        String signature = entity.getHeaders().getFirst("X-Webhook-Signature");
                        return contentType.contains("application/json") &&
                                expectedSignature.equals(signature);
                    }),
                    eq(MLQueueResponse.class)
            );
        }
    }

    @Test
    void ping_shouldReturnResponseFromMLService() {
        // Arrange
        MLPingResponse expectedPingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> responseEntity = new ResponseEntity<>(expectedPingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.getForEntity(
                            eq("http://ml-service.test/ping"),
                            eq(MLPingResponse.class)
                    )).thenReturn(responseEntity);
                })) {

            // Act
            ResponseEntity<MLPingResponse> actualResponse = queryService.ping();

            // Assert
            assertNotNull(actualResponse);
            assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
            assertEquals(expectedPingResponse, actualResponse.getBody());
            assertTrue(actualResponse.getBody().ok());

            RestTemplate mockRestTemplate = mockedConstruction.constructed().get(0);
            verify(mockRestTemplate).getForEntity("http://ml-service.test/ping", MLPingResponse.class);
        }
    }

    @Test
    void ping_shouldConstructCorrectUrl() {
        // Arrange
        when(mlServiceProps.getUrl()).thenReturn("http://different-ml-service.test");

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> responseEntity = new ResponseEntity<>(pingResponse, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.getForEntity(
                            anyString(),
                            eq(MLPingResponse.class)
                    )).thenReturn(responseEntity);
                })) {

            // Act
            queryService.ping();

            // Assert
            RestTemplate mockRestTemplate = mockedConstruction.constructed().get(0);
            verify(mockRestTemplate).getForEntity(
                    eq("http://different-ml-service.test/ping"),
                    eq(MLPingResponse.class)
            );
        }
    }
}
