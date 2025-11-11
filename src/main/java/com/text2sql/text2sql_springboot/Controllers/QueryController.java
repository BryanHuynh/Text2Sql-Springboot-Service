package com.text2sql.text2sql_springboot.Controllers;

import com.text2sql.text2sql_springboot.DTO.QueryRequest;
import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    public QueryController() {
    }

    @PostMapping()
    public ResponseEntity<String> query(@Valid @RequestBody QueryRequest req) {
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}
