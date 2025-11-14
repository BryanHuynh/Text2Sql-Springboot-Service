package com.text2sql.text2sql_springboot.DTO;

import java.util.Map;
import java.util.UUID;

public class QuerySchemaRequest {
    private Map<String, Object> schema;
    private String question;
    private String dbId;
    private String callbackUrl;
    private UUID id;

    public QuerySchemaRequest(Builder builder) {
        this.schema = builder.getSchema();
        this.question = builder.getQuestion();
        this.dbId = builder.getDbId();
        this.callbackUrl = builder.getCallbackUrl();
        this.id = builder.id;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDbId() {
        return dbId;
    }

    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


    public static class Builder {
        private Map<String, Object> schema;
        private String question;
        private String dbId;
        private String callbackUrl;
        private UUID id;

        public Builder() {
        }

        public Builder schema(Map<String, Object> schema) {
            this.schema = schema;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder dbId(String dbId) {
            this.dbId = dbId;
            return this;
        }

        public Builder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public Builder id(UUID correlationId) {
            this.id = correlationId;
            return this;
        }

        public QuerySchemaRequest build() {
            return new QuerySchemaRequest(this);
        }

        public Map<String, Object> getSchema() {
            return schema;
        }

        public String getQuestion() {
            return question;
        }

        public String getDbId() {
            return dbId;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public UUID getId() {
            return id;
        }
    }
}
