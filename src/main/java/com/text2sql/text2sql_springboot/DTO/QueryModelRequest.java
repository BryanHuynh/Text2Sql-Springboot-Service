package com.text2sql.text2sql_springboot.DTO;

import java.util.List;
import java.util.UUID;

public class QueryModelRequest {
    private UUID id;
    private String question;
    private String db_id;
    private SchemaModel schema;
}

