package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private MLServiceClient mlServiceClient;

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
                mlServiceClient,
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
    }

    @Test
    void query_shouldThrowException_whenDatabaseNotFound() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.empty());

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryService.query(testQueryRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Database not found", exception.getReason());

        verify(mlServiceClient).ping();
        verify(userDatabaseRepository).findById(testDatabaseId);
        verifyNoInteractions(smConstructionService, httpConstructionService, pendingJobsRepository);
        verifyNoMoreInteractions(mlServiceClient);
    }

    @Test
    void query_shouldThrowException_whenPingFails() {
        // Arrange
        MLPingResponse pingResponse = new MLPingResponse(false, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.SERVICE_UNAVAILABLE);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryService.query(testQueryRequest)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());

        verify(mlServiceClient).ping();
        verifyNoInteractions(userDatabaseRepository,
                             smConstructionService,
                             httpConstructionService,
                             pendingJobsRepository);
        verifyNoMoreInteractions(mlServiceClient);
    }

    @Test
    void query_shouldSuccessfullyQueueJob_whenAllConditionsAreMet() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(
                testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);
        when(pendingJobsRepository.save(any(PendingJobs.class))).thenAnswer(invocation -> invocation.getArgument(
                0));

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        MLQueueResponse successResponse = new MLQueueResponse(true,
                                                              MLQueueStatusResponses.queued,
                                                              "Job queued successfully");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(successResponse,
                                                                              HttpStatus.OK);
        when(mlServiceClient.queueJob(testQuerySchemaRequest)).thenReturn(responseEntity);

        // Act
        queryService.query(testQueryRequest);

        // Assert
        verify(mlServiceClient).ping();
        verify(userDatabaseRepository).findById(testDatabaseId);
        verify(smConstructionService).constructSchema(testUserDatabase);
        verify(httpConstructionService).constructHttpRequest(testQueryRequest, testSchemaModel);
        verify(mlServiceClient).queueJob(testQuerySchemaRequest);
        verify(pendingJobsRepository).save(argThat(pendingJob ->
                                                           pendingJob.getCorrelationId()
                                                                   .equals(testCorrelationId) &&
                                                                   pendingJob.getUserDetail()
                                                                           .equals(testUserDetail) &&
                                                                   pendingJob.getJobStatus() == JobStatus.STARTED
        ));
        verify(pendingJobsRepository, times(1)).save(any(PendingJobs.class));
    }

    @Test
    void query_shouldNotSavePendingJob_whenResponseIsNotSuccessful() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(
                testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        MLQueueResponse failureResponse = new MLQueueResponse(true,
                                                              MLQueueStatusResponses.queued,
                                                              "Job queued");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(failureResponse,
                                                                              HttpStatus.INTERNAL_SERVER_ERROR);
        when(mlServiceClient.queueJob(testQuerySchemaRequest)).thenReturn(responseEntity);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryService.query(testQueryRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(mlServiceClient).ping();
        verify(userDatabaseRepository).findById(testDatabaseId);
        verify(smConstructionService).constructSchema(testUserDatabase);
        verify(httpConstructionService).constructHttpRequest(testQueryRequest, testSchemaModel);
        verify(mlServiceClient).queueJob(testQuerySchemaRequest);
        verifyNoInteractions(pendingJobsRepository);
    }

    @Test
    void query_shouldNotSavePendingJob_whenResponseBodyIsNull() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(
                testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(mlServiceClient.queueJob(testQuerySchemaRequest)).thenReturn(responseEntity);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryService.query(testQueryRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(mlServiceClient).ping();
        verify(mlServiceClient).queueJob(testQuerySchemaRequest);
        verify(userDatabaseRepository).findById(testDatabaseId);
        verifyNoInteractions(pendingJobsRepository);
    }

    @Test
    void query_shouldNotSavePendingJob_whenOkIsFalse() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(
                testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        MLQueueResponse failureResponse = new MLQueueResponse(false,
                                                              MLQueueStatusResponses.queued,
                                                              "Error occurred");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(failureResponse,
                                                                              HttpStatus.OK);
        when(mlServiceClient.queueJob(testQuerySchemaRequest)).thenReturn(responseEntity);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queryService.query(testQueryRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(mlServiceClient).ping();
        verify(mlServiceClient).queueJob(testQuerySchemaRequest);
        verify(userDatabaseRepository).findById(testDatabaseId);
        verifyNoInteractions(pendingJobsRepository);
    }

    @Test
    void query_shouldNotSavePendingJob_whenStatusIsNotQueued() throws JsonProcessingException {
        // Arrange
        when(userDatabaseRepository.findById(testDatabaseId)).thenReturn(Optional.of(
                testUserDatabase));
        when(smConstructionService.constructSchema(testUserDatabase)).thenReturn(testSchemaModel);
        when(httpConstructionService.constructHttpRequest(testQueryRequest, testSchemaModel))
                .thenReturn(testQuerySchemaRequest);

        MLPingResponse pingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> pingResponseEntity = new ResponseEntity<>(pingResponse,
                                                                                 HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(pingResponseEntity);

        MLQueueResponse processingResponse = new MLQueueResponse(true,
                                                                 MLQueueStatusResponses.processing,
                                                                 "Already processing");
        ResponseEntity<MLQueueResponse> responseEntity = new ResponseEntity<>(processingResponse,
                                                                              HttpStatus.OK);
        when(mlServiceClient.queueJob(testQuerySchemaRequest)).thenReturn(responseEntity);

        queryService.query(testQueryRequest);


        verify(mlServiceClient).ping();
        verify(mlServiceClient).queueJob(testQuerySchemaRequest);
        verify(userDatabaseRepository).findById(testDatabaseId);
        verifyNoInteractions(pendingJobsRepository);
    }

    @Test
    void ping_shouldReturnResponseFromMLService() {
        // Arrange
        MLPingResponse expectedPingResponse = new MLPingResponse(true, LocalDateTime.now());
        ResponseEntity<MLPingResponse> responseEntity = new ResponseEntity<>(expectedPingResponse,
                                                                             HttpStatus.OK);
        when(mlServiceClient.ping()).thenReturn(responseEntity);

        // Act
        ResponseEntity<MLPingResponse> actualResponse = queryService.ping();

        // Assert
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(expectedPingResponse, actualResponse.getBody());
        assertTrue(actualResponse.getBody().ok());
        verify(mlServiceClient).ping();
    }
}
