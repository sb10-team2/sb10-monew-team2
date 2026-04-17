package com.springboot.monew.interest.controller;

import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.service.InterestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController implements InterestApiDocs {

    private final InterestService interestService;

    @PostMapping
    public ResponseEntity<InterestDto> create(@Valid @RequestBody InterestRegisterRequest request) {
        InterestDto interestDto = interestService.create(request);
        return ResponseEntity.created(URI.create("/api/interests")).body(interestDto);
    }
}
