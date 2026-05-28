package com.insuranceiq.controller;

import com.insuranceiq.dto.PolicyRequest;
import com.insuranceiq.dto.PolicyResponse;
import com.insuranceiq.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Policy management endpoints")
public class PolicyController {

    private final PolicyService policyService;
    private final com.insuranceiq.repository.PolicyRepository policyRepository;

    @GetMapping
    @Operation(summary = "Get all policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    public ResponseEntity<List<PolicyResponse>> getAll() {
        return ResponseEntity.ok(policyService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<PolicyResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(policyService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<PolicyResponse> create(@RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<PolicyResponse> update(@PathVariable String id, @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/renewals/upcoming")
    @Operation(summary = "Get policies due for renewal within 30 days")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        List<Map<String, Object>> renewals = policyRepository.findAll().stream()
                .filter(p -> p.getEndDate() != null
                        && !p.getEndDate().isBefore(today)
                        && !p.getEndDate().isAfter(threshold))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("policyId", p.getPolicyId());
                    m.put("customerId", p.getCustomer() != null ? p.getCustomer().getUser().getId() : null);
                    m.put("agentId", p.getAgent() != null ? p.getAgent().getUser().getId() : null);
                    m.put("daysRemaining", (int) ChronoUnit.DAYS.between(today, p.getEndDate()));
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(renewals);
    }
}
