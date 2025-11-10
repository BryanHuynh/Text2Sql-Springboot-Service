package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.MLPingDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;


import java.net.InetAddress;
import java.net.UnknownHostException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest()
@ActiveProfiles({"test", "test-local"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryServiceIntegrationTest {

    @Autowired
    private QueryService queryService;

    private static final String WEBHOOK_SECRET = "test-secret-key-12345";


    @Test
    @Order(1)
    @DisplayName("Test External Service with ping")
    public void ShouldReturnTrue_ping_WhenExternalServicePinged() {
        ResponseEntity<MLPingDto> response = queryService.ping();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().ok());
        assertEquals(true, response.getBody().ok());
        assertNotNull(response.getBody().ackTime());

        System.out.println("endpoint successfully received request via get");
    }

    @Test
    @Order(2)
    @DisplayName("Text External Service to queue payload")
    public void ShouldReturnACK_Queue_WhenServiceCalledWithPayload() throws UnknownHostException {


    }


}