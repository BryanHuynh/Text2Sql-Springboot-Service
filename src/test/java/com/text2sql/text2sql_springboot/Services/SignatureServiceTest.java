package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceTest {
    private static SignatureService signatureService;
    private static String payload;
    private static String validSignature;

    @BeforeAll
    static void setupProps() throws JsonProcessingException {
        MLServiceProps mlServiceProps = new MLServiceProps();
        mlServiceProps.setSecret("TEST-123");
        signatureService = new SignatureService(mlServiceProps);
        validSignature = "d0af5d7415b8192d579acf9fc7388e26b1b1f8aa83716ca2accad7996822368b";
        String rawPayload = """
                {
                    "id": "f23f915a-24b6-4278-a2d3-92b409eace15",
                    "question": "get all warehouses with locations in calgary",
                    "db_id": "warehouse_1",
                    "schema": {
                        "column_names_original": [
                            [-1, "*"],
                            [0, "Code"],
                            [0, "Location"],
                            [0, "Capacity"],
                            [1, "Code"],
                            [1, "Contents"],
                            [1, "Value"],
                            [1, "Warehouse"]
                        ],
                        "column_types": ["text", "number", "text", "number", "text", "text", "number", "number"],
                        "table_names_original": ["Warehouses", "Boxes"],
                        "foreign_keys": [[7, 1]],
                        "primary_keys": [1, 4],
                        "db_id": "warehouse_1"
                    }
                }""";
        ObjectMapper mapper = new ObjectMapper();
        payload = mapper.writeValueAsString(
                mapper.readTree(rawPayload)
        );
    }

    @Test
    void ShouldReturnValidSignature_generateSignature_whenGivenPayload() throws JsonProcessingException {
        assertEquals(validSignature, signatureService.generateSignature(payload));
    }

    @Test
    void ShouldReturnTrue_verifySignature_whenGivenProperSignature() throws JsonProcessingException {
        assertTrue(signatureService.verifySignature(payload, validSignature));
    }

    @Test
    void ShouldReturnFalse_verifySignature_whenGivenWrongSignature() throws JsonProcessingException {
        assertFalse(signatureService.verifySignature(payload, "Bad-signature"));
    }


}