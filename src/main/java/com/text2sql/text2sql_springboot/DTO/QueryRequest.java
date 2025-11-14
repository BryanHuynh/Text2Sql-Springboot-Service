package com.text2sql.text2sql_springboot.DTO;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class QueryRequest {


    @NotBlank
    private String question;

    @NotBlank
    UUID database_id;

    public QueryRequest(String question, UUID database_id) {
        this.question = question;
        this.database_id = database_id;
    }

    public String getQuestion() {
        return question;
    }

    public UUID getDatabase_id() {
        return database_id;
    }

}
