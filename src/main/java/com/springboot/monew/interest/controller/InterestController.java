package com.springboot.monew.interest.controller;

import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.service.InterestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController implements InterestApiDocs {

  private final InterestService interestService;

  @GetMapping
  public CursorPageResponseInterestDto list(@Valid InterestPageRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    return interestService.list(request, userId);
  }

  @PostMapping
  public ResponseEntity<InterestDto> create(@Valid @RequestBody InterestRegisterRequest request) {
    InterestDto interestDto = interestService.create(request);
    return ResponseEntity.created(URI.create("/api/interests/" + interestDto.id()))
        .body(interestDto);
  }

  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestDto> update(@PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request) {
    InterestDto interestDto = interestService.update(interestId, request);
    return ResponseEntity.ok(interestDto);
  }

  @DeleteMapping("/{interestId}")
  public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }
}
