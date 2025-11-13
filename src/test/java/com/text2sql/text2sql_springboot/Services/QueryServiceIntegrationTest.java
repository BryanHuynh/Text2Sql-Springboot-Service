package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import com.text2sql.text2sql_springboot.DTO.MLPingResponse;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;


import java.net.UnknownHostException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class QueryServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private QueryService queryService;

//    @MockBean
//    private MLServiceClient mlServiceClient;

    @Autowired
    private PendingJobsRepository pendingJobsRepository;

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private SchemaModelConstructionService smConstructionService;

    @Autowired
    private QueryConstructionService httpConstructionService;

    @Autowired
    private UserDatabaseRepository userDatabaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTableRepository userTableRepository;

    @Autowired
    private TableVariablesRepository tableVariablesRepository;

    @BeforeEach
    public void setupTestData() {
        UserDetail user = new UserDetail("test_id_123", "Test@email.com");
        userRepository.saveAndFlush(user);


        UserDatabase db = new UserDatabase("schools", user);
        userDatabaseRepository.saveAndFlush(db);

        UserTable studentTable = new UserTable("students", db);
        UserTable schoolTable = new UserTable("schools", db);
        userTableRepository.saveAndFlush(studentTable);
        userTableRepository.saveAndFlush(schoolTable);

        TableVariable schoolIdVariable = new TableVariable.Builder()
                .variableName("id")
                .variableType("UUID")
                .pkFlag(true)
                .fkFlag(false)
                .userTable(schoolTable)
                .build();
        tableVariablesRepository.saveAndFlush(schoolIdVariable);

        TableVariable schoolNameVariable = new TableVariable.Builder()
                .variableName("name")
                .variableType("String")
                .pkFlag(false)
                .fkFlag(false)
                .userTable(schoolTable)
                .build();
        tableVariablesRepository.saveAndFlush(schoolNameVariable);

        TableVariable studentIdVariable = new TableVariable.Builder()
                .variableName("id")
                .variableType("UUID")
                .pkFlag(true)
                .fkFlag(false)
                .userTable(studentTable)
                .build();
        tableVariablesRepository.saveAndFlush(studentIdVariable);

        TableVariable studentNameVariable = new TableVariable.Builder()
                .variableName("name")
                .variableType("String")
                .pkFlag(false)
                .fkFlag(false)
                .userTable(studentTable)
                .build();
        tableVariablesRepository.saveAndFlush(studentNameVariable);

        TableVariable studentsSchoolVariable = new TableVariable.Builder()
                .variableName("school")
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

        // Verify relationships
        UserDatabase db = userDatabaseRepository.findAll().get(0);
        assertNotNull(db.getUser());

        System.out.println("All test data verified");
    }

//    @Test
//    @Order(1)
//    @DisplayName("Test External Service with ping")
//    public void ShouldReturnTrue_ping_WhenExternalServicePinged() {
//        ResponseEntity<MLPingResponse> response = queryService.ping();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().ok());
//        assertEquals(true, response.getBody().ok());
//        assertNotNull(response.getBody().ackTime());
//
//        System.out.println("endpoint successfully received request via get");
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("Text External Service to queue payload")
//    public void ShouldReturnACK_Queue_WhenServiceCalledWithPayload() throws UnknownHostException {
//
//
//    }
//

}