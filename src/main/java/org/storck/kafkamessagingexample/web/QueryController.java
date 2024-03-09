package org.storck.kafkamessagingexample.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.storck.kafkamessagingexample.service.QueryService;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryService queryService;

    @Autowired
    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }
    
    @PostMapping("/process")
    @Operation(summary = "Process a local query", description = "This method processes a local query")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    public ResponseEntity<List<String>> processLocalQuery(@RequestBody String query) throws ExecutionException, InterruptedException {
        List<String> response = queryService.processLocalQuery(query);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}