package com.insuranceiq.controller;

import com.insuranceiq.model.Payment;
import com.insuranceiq.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping
    @Operation(summary = "Get all payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLAIMS_MANAGER', 'AGENT')")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> payments = paymentRepository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new com.insuranceiq.exception.ResourceNotFoundException("Payment not found: " + id));
        return ResponseEntity.ok(toMap(payment));
    }

    private Map<String, Object> toMap(Payment p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("payment_id", p.getPaymentId());
        map.put("policy_id", p.getPolicy() != null ? p.getPolicy().getPolicyId() : null);
        map.put("customer_name", p.getCustomerName());
        map.put("amount", p.getAmount());
        map.put("payment_date", p.getPaymentDate() != null ? p.getPaymentDate().toString() : null);
        map.put("type", p.getType() != null ? p.getType().name().toLowerCase() : null);
        map.put("status", p.getStatus() != null ? p.getStatus().name().toLowerCase() : null);
        return map;
    }
}
