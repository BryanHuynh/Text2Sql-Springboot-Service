package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.JobStatus;
import com.text2sql.text2sql_springboot.DTO.MLPingResponse;
import com.text2sql.text2sql_springboot.DTO.QueryRequest;
import com.text2sql.text2sql_springboot.Entities.*;
import com.text2sql.text2sql_springboot.Repositories.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;


import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class QueryServiceIntegrationTest {

    @Autowired
    private QueryService queryService;

    @MockitoBean
    private MLServiceProps mlServiceProps;

    @Autowired
    private PendingJobsRepository pendingJobsRepository;

    @Autowired
    private UserDatabaseRepository userDatabaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTableRepository userTableRepository;

    @Autowired
    private TableVariablesRepository tableVariablesRepository;

    private static WireMockServer mlMockServer;

    @BeforeAll
    public static void setupMlServerMock() {
        mlMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        mlMockServer.start();

        WireMock.configureFor("localhost", mlMockServer.port());

        System.out.println("WireMock started on port: " + mlMockServer.port());
    }

    @AfterAll
    public static void stopWireMock() {
        if (mlMockServer != null && mlMockServer.isRunning()) {
            mlMockServer.stop();
            System.out.println("WireMock stopped");
        }
    }

    @BeforeEach
    public void setupTestData() {
        mlMockServer.resetAll();

        when(mlServiceProps.getUrl()).thenReturn("http://localhost:" + mlMockServer.port());
        when(mlServiceProps.getSecret()).thenReturn("test-secret");

        UserDetail user = new UserDetail("test_id_123", "Test@email.com");
        userRepository.saveAndFlush(user);


        UserDatabase db = new UserDatabase("schools", user);
        userDatabaseRepository.saveAndFlush(db);

        UserTable studentTable = new UserTable("students", db);
        UserTable schoolTable = new UserTable("schools", db);
        userTableRepository.saveAndFlush(studentTable);
        userTableRepository.saveAndFlush(schoolTable);

        TableVariable schoolIdVariable = new TableVariable.Builder().variableName("id")
                .variableType("UUID")
                .pkFlag(true)
                .fkFlag(false)
                .userTable(schoolTable)
                .build();
        tableVariablesRepository.saveAndFlush(schoolIdVariable);

        TableVariable schoolNameVariable = new TableVariable.Builder().variableName("name")
                .variableType("String")
                .pkFlag(false)
                .fkFlag(false)
                .userTable(schoolTable)
                .build();
        tableVariablesRepository.saveAndFlush(schoolNameVariable);

        TableVariable studentIdVariable = new TableVariable.Builder().variableName("id")
                .variableType("UUID")
                .pkFlag(true)
                .fkFlag(false)
                .userTable(studentTable)
                .build();
        tableVariablesRepository.saveAndFlush(studentIdVariable);

        TableVariable studentNameVariable = new TableVariable.Builder().variableName("name")
                .variableType("String")
                .pkFlag(false)
                .fkFlag(false)
                .userTable(studentTable)
                .build();
        tableVariablesRepository.saveAndFlush(studentNameVariable);

        TableVariable studentsSchoolVariable = new TableVariable.Builder().variableName("school")
                .variableType("UUID")
                .pkFlag(false)
                .fkFlag(true)
                .fkRef(schoolIdVariable)
                .userTable(studentTable)
                .build();
        tableVariablesRepository.saveAndFlush(studentsSchoolVariable);
    }


    @Test
    public void testQuery() {
        assertEquals(1, userRepository.count());
        assertEquals(1, userDatabaseRepository.count());
        assertEquals(2, userTableRepository.count());
        assertEquals(5, tableVariablesRepository.count());

        UserDatabase db = userDatabaseRepository.findAll().get(0);
        assertNotNull(db.getUser());

        System.out.println("All test data verified");
    }

    @Test
    public void givenMLServerIsLive_ping_ShouldReturnSuccessCode() {
        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse().withStatus(200)));
        ResponseEntity<MLPingResponse> response = queryService.ping();
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void givenMLServerIsLiveAndRequestValid_query_ShouldPostCorrectRequestAndLog() throws JsonProcessingException {
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        String question = "Test Question";
        QueryRequest queryRequest = new QueryRequest(question, db.getId());

        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse().withStatus(200)));
        mlMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/queue"))
                                     .withHeader("Content-Type", equalTo("application/json"))
                                     .withHeader("X-Webhook-Signature", matching(".+"))
                                     .withRequestBody(WireMock.matchingJsonPath("$.schema",
                                                                                matching(".+")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.question",
                                                                                equalTo(question)))
                                     .withRequestBody(WireMock.matchingJsonPath("$.dbId",
                                                                                equalTo(db.getDatabaseName())))
                                     .withRequestBody(WireMock.matchingJsonPath("$.callbackUrl",
                                                                                matching(
                                                                                        ".+/query/jobs/.+/callback")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.id",
                                                                                matching(".+")))
                                     .willReturn(WireMock.aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Type",
                                                                     "application/json")
                                                         .withBody("""
                                                                           {
                                                                               "ok": true,
                                                                               "status": "queued",
                                                                               "msg": "Request has been queued"
                                                                           }
                                                                           """)));


        queryService.query(queryRequest);
        assertEquals(1, pendingJobsRepository.count());
        PendingJobs pendingJobs = pendingJobsRepository.findAll().get(0);
        assertEquals(JobStatus.STARTED, pendingJobs.getJobStatus());
        assertEquals(db.getUser().getId(), pendingJobs.getUserDetail().getId());
    }

    @Test
    public void givenMLServerIsDownAndRequestValid_query_ShouldThrowException() throws JsonProcessingException {
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        String question = "Test Question";
        QueryRequest queryRequest = new QueryRequest(question, db.getId());

        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse()
                                                         .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> queryService.query(queryRequest));

        assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());
        assertEquals(SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    public void givenMLServerIsLiveButInternalServerIssue_query_ShouldThrowException() throws JsonProcessingException {
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        String question = "Test Question";
        QueryRequest queryRequest = new QueryRequest(question, db.getId());

        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse().withStatus(200)));

        mlMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/queue"))
                                     .withHeader("Content-Type", equalTo("application/json"))
                                     .withHeader("X-Webhook-Signature", matching(".+"))
                                     .withRequestBody(WireMock.matchingJsonPath("$.schema",
                                                                                matching(".+")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.question",
                                                                                equalTo(question)))
                                     .withRequestBody(WireMock.matchingJsonPath("$.dbId",
                                                                                equalTo(db.getDatabaseName())))
                                     .withRequestBody(WireMock.matchingJsonPath("$.callbackUrl",
                                                                                matching(
                                                                                        ".+/query/jobs/.+/callback")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.id",
                                                                                matching(".+")))
                                     .willReturn(WireMock.aResponse()
                                                         .withStatus(SERVICE_UNAVAILABLE.value())));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> queryService.query(queryRequest));

        assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    public void givenMLServerIsLiveButReturnsErrorResponse_query_ShouldThrowException() throws JsonProcessingException {
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        String question = "Test Question";
        QueryRequest queryRequest = new QueryRequest(question, db.getId());

        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse().withStatus(200)));

        mlMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/queue"))
                                     .withHeader("Content-Type", equalTo("application/json"))
                                     .withHeader("X-Webhook-Signature", matching(".+"))
                                     .withRequestBody(WireMock.matchingJsonPath("$.schema",
                                                                                matching(".+")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.question",
                                                                                equalTo(question)))
                                     .withRequestBody(WireMock.matchingJsonPath("$.dbId",
                                                                                equalTo(db.getDatabaseName())))
                                     .withRequestBody(WireMock.matchingJsonPath("$.callbackUrl",
                                                                                matching(
                                                                                        ".+/query/jobs/.+/callback")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.id",
                                                                                matching(".+")))
                                     .willReturn(WireMock.aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Type",
                                                                     "application/json")
                                                         .withBody("""
                                                                           {
                                                                               "ok": false,
                                                                               "status": "error",
                                                                               "msg": "Internal processing error occurred"
                                                                           }
                                                                           """)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> queryService.query(queryRequest));

        assertEquals("There was an error upstream", exception.getReason());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals(0, pendingJobsRepository.count());
    }

    @Test
    public void givenMLServerHasReadTimeout_query_ShouldThrowException() throws JsonProcessingException {
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        String question = "Test Question";
        QueryRequest queryRequest = new QueryRequest(question, db.getId());

        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse().withStatus(200)));

        mlMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/queue"))
                                     .withHeader("Content-Type", equalTo("application/json"))
                                     .withHeader("X-Webhook-Signature", matching(".+"))
                                     .withRequestBody(WireMock.matchingJsonPath("$.schema",
                                                                                matching(".+")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.question",
                                                                                equalTo(question)))
                                     .withRequestBody(WireMock.matchingJsonPath("$.dbId",
                                                                                equalTo(db.getDatabaseName())))
                                     .withRequestBody(WireMock.matchingJsonPath("$.callbackUrl",
                                                                                matching(
                                                                                        ".+/query/jobs/.+/callback")))
                                     .withRequestBody(WireMock.matchingJsonPath("$.id",
                                                                                matching(".+")))
                                     .willReturn(WireMock.aResponse()
                                                         .withStatus(200)
                                                         .withFixedDelay(12000)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> queryService.query(queryRequest));

        assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());
        assertEquals(SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals(0, pendingJobsRepository.count());
    }

    @Test
    public void givenMLServerHasReadTimeout_ping_ShouldThrowException() {
        mlMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/ping"))
                                     .willReturn(WireMock.aResponse()
                                                         .withStatus(200)
                                                         .withFixedDelay(12000)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> queryService.ping());

        assertEquals("Upstream Server unreachable. Please try again later", exception.getReason());
        assertEquals(SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

}