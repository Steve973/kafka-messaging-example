package org.storck.kafkamessagingexample.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.storck.kafkamessagingexample.service.QueryService;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }
    
    @PostMapping("/process")
    @Operation(summary = "Process a local query", description = "This method processes a local query")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PreAuthorize("hasAnyAuthority('Auth1','Auth4')")
    public ResponseEntity<List<String>> processLocalQuery(@RequestBody String query) throws Exception {
        List<String> response = queryService.processLocalQuery(query, Duration.ofSeconds(5));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/check")
    @PreAuthorize("hasAuthority('Auth4')")
    public ResponseEntity<String> checkSecurity() {
        return new ResponseEntity<>("Result: success", HttpStatus.OK);
    }
}