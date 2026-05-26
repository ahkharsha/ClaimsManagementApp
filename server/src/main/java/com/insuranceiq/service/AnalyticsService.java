package com.insuranceiq.service;

import com.insuranceiq.dto.DashboardSummary;
import com.insuranceiq.model.enums.ClaimStatus;
import com.insuranceiq.model.enums.PolicyStatus;
import com.insuranceiq.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final AgentRepository agentRepository;
    private final FraudPredictionRepository fraudPredictionRepository;

    public DashboardSummary getDashboardSummary() {
        return DashboardSummary.builder()
                .totalPolicies(policyRepository.count())
                .activePolicies(policyRepository.countByStatus(PolicyStatus.ACTIVE))
                .totalClaims(claimRepository.count())
                .pendingClaims(claimRepository.countByStatus(ClaimStatus.PENDING))
                .approvedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED))
                .rejectedClaims(claimRepository.countByStatus(ClaimStatus.REJECTED))
                .totalPremium(policyRepository.sumAllPremiums())
                .fraudAlerts(fraudPredictionRepository.findByFraudProbabilityGreaterThan(60).size())
                .build();
    }

    public List<Map<String, Object>> getTopAgents() {
        return agentRepository.findAll().stream()
                .sorted((a, b) -> Double.compare(
                        b.getTotalPremium() != null ? b.getTotalPremium() : 0,
                        a.getTotalPremium() != null ? a.getTotalPremium() : 0))
                .limit(10)
                .map(agent -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", agent.getName());
                    map.put("policies", agent.getPoliciesSold());
                    map.put("premium", agent.getTotalPremium());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getClaimsTrend() {
        // Aggregate actual claims by month from the DB
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> trend = new ArrayList<>();

        // Group all claims by their created_at month
        Map<Integer, List<com.insuranceiq.model.Claim>> byMonth = claimRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().getMonthValue()));

        for (int i = 1; i <= 12; i++) {
            List<com.insuranceiq.model.Claim> claims = byMonth.getOrDefault(i, List.of());
            long filed = claims.size();
            long approved = claims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
            long rejected = claims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
            long settled = claims.stream().filter(c -> c.getStatus() == ClaimStatus.SETTLED).count();

            if (filed > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("month", months[i - 1]);
                m.put("filed", filed);
                m.put("approved", approved);
                m.put("rejected", rejected);
                m.put("settled", settled);
                trend.add(m);
            }
        }

        return trend;
    }

    public List<Map<String, Object>> getFraudFlaggedClaims() {
        return claimRepository.findByFraudScoreGreaterThan(60).stream()
                .map(claim -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("claim_id", claim.getClaimId());
                    map.put("customer_name", claim.getCustomer() != null ? claim.getCustomer().getName() : null);
                    map.put("claim_amount", claim.getClaimAmount());
                    map.put("fraud_score", claim.getFraudScore());
                    map.put("status", claim.getStatus() != null ? claim.getStatus().toValue() : null);
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getLossRatio() {
        Double totalPremiums = policyRepository.sumAllPremiums();
        Double totalPayouts = claimRepository.sumSettledClaimAmounts();
        double lossRatio = totalPremiums > 0 ? (totalPayouts / totalPremiums) * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_premiums", totalPremiums);
        result.put("total_payouts", totalPayouts);
        result.put("loss_ratio_pct", Math.round(lossRatio * 100.0) / 100.0);
        return result;
    }

    public Map<String, Object> getRenewalRate() {
        long total = policyRepository.count();
        long active = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        long expired = policyRepository.countByStatus(PolicyStatus.EXPIRED);
        long renewalDue = policyRepository.countByStatus(PolicyStatus.RENEWAL_DUE);
        double renewalRate = total > 0 ? ((double) active / total) * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_policies", total);
        result.put("active", active);
        result.put("expired", expired);
        result.put("renewal_due", renewalDue);
        result.put("renewal_rate_pct", Math.round(renewalRate * 100.0) / 100.0);
        return result;
    }

    public List<Map<String, Object>> getFraudDistribution() {
        long low = fraudPredictionRepository.findAll().stream().filter(f -> f.getFraudProbability() != null && f.getFraudProbability() <= 30).count();
        long medium = fraudPredictionRepository.findAll().stream().filter(f -> f.getFraudProbability() != null && f.getFraudProbability() > 30 && f.getFraudProbability() <= 70).count();
        long high = fraudPredictionRepository.findAll().stream().filter(f -> f.getFraudProbability() != null && f.getFraudProbability() > 70).count();

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> lowMap = new LinkedHashMap<>(); lowMap.put("name", "Low Risk"); lowMap.put("value", low); lowMap.put("fill", "#10b981"); result.add(lowMap);
        Map<String, Object> medMap = new LinkedHashMap<>(); medMap.put("name", "Medium Risk"); medMap.put("value", medium); medMap.put("fill", "#f59e0b"); result.add(medMap);
        Map<String, Object> highMap = new LinkedHashMap<>(); highMap.put("name", "High Risk"); highMap.put("value", high); highMap.put("fill", "#ef4444"); result.add(highMap);
        return result;
    }

    public List<Map<String, Object>> getPolicyByType() {
        return policyRepository.findAll().stream()
                .filter(p -> p.getProduct() != null && p.getProduct().getType() != null)
                .collect(Collectors.groupingBy(p -> p.getProduct().getType().toValue()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", e.getKey());
                    map.put("count", e.getValue().size());
                    map.put("premium", e.getValue().stream().mapToDouble(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : 0).sum());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRenewalTrend() {
        // Aggregate real policy statuses by end_date month
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> trend = new ArrayList<>();

        Map<Integer, List<com.insuranceiq.model.Policy>> byMonth = policyRepository.findAll().stream()
                .filter(p -> p.getEndDate() != null)
                .collect(Collectors.groupingBy(p -> p.getEndDate().getMonthValue()));

        for (int i = 1; i <= 12; i++) {
            List<com.insuranceiq.model.Policy> policies = byMonth.getOrDefault(i, List.of());
            if (policies.isEmpty()) continue;

            long renewed = policies.stream().filter(p -> p.getStatus() == PolicyStatus.ACTIVE).count();
            long lapsed = policies.stream().filter(p -> p.getStatus() == PolicyStatus.EXPIRED).count();
            long due = policies.stream().filter(p -> p.getStatus() == PolicyStatus.RENEWAL_DUE).count();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", months[i - 1]);
            m.put("renewed", renewed);
            m.put("lapsed", lapsed);
            m.put("due", due);
            trend.add(m);
        }
        return trend;
    }

    public List<Map<String, Object>> getCommissionData() {
        // Compute actual commission data from agents table
        return agentRepository.findAll().stream()
                .filter(a -> a.getTotalPremium() != null && a.getCommissionPct() != null)
                .map(agent -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("agent", agent.getName());
                    double earned = agent.getTotalPremium() * (agent.getCommissionPct() / 100.0);
                    m.put("earned", Math.round(earned));
                    // Pending = policies sold this period but not yet paid out (estimate ~20% pending)
                    m.put("pending", Math.round(earned * 0.2));
                    m.put("policies_sold", agent.getPoliciesSold());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
