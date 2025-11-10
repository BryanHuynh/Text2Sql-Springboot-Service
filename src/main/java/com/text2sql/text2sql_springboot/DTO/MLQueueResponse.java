package com.text2sql.text2sql_springboot.DTO;

public record MLQueueResponse(
        Boolean ok,
        MLQueueStatusResponses status,
        String msg
) {
}
