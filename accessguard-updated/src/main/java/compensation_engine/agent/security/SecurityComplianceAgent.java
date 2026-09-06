package compensation_engine.agent.security;

import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.model.AccessPolicy;
import compensation_engine.model.SodRule;
import compensation_engine.repository.AccessPolicyRepository;
import compensation_engine.repository.SodRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SecurityComplianceAgent {

    private final AccessPolicyRepository accessPolicyRepository;
    private final SodRuleRepository sodRuleRepository;

    public SecurityComplianceAgent() {
        this.accessPolicyRepository = null;
        this.sodRuleRepository = null;
    }

    @Autowired
    public SecurityComplianceAgent(AccessPolicyRepository accessPolicyRepository,
                                  SodRuleRepository sodRuleRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.sodRuleRepository = sodRuleRepository;
    }

    public SecurityEvaluation evaluate(OnboardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Onboarding request cannot be null.");
        }

        List<String> violations = new ArrayList<>();
        int score = 0;
        String department = normalize(request.getDepartment());
        String role = normalize(request.getRole());
        List<AccessGrantRequest> accessRequests = accessRequests(request);

        if (department.isBlank() || role.isBlank()) {
            violations.add("Department and role are required for a complete policy evaluation.");
            score += 30;
        }

        if (accessRequests.isEmpty()) {
            violations.add("At least one application access request is required.");
            score += 30;
        }

        // DB Policy Evaluation if repositories are present
        boolean forceApproval = false;
        boolean forceDualApproval = false;

        if (accessPolicyRepository != null) {
            List<AccessPolicy> policies = accessPolicyRepository.findByEnabledTrue();
            for (AccessGrantRequest access : accessRequests) {
                String app = normalize(access.getApplication());
                String lvl = normalize(access.getAccessLevel());
                for (AccessPolicy pol : policies) {
                    boolean deptMatch = pol.getDepartment().equals("*") || normalize(pol.getDepartment()).equals(department);
                    boolean roleMatch = pol.getRole().equals("*") || normalize(pol.getRole()).equals(role);
                    boolean appMatch  = pol.getApplication().equals("*") || normalize(pol.getApplication()).equals(app);
                    boolean lvlMatch  = pol.getMaxAccessLevel().equals("*") || normalize(pol.getMaxAccessLevel()).equals(lvl);

                    if (deptMatch && roleMatch && appMatch && lvlMatch) {
                        if (pol.isRequiresApproval()) forceApproval = true;
                        if (pol.isRequiresDualApproval()) forceDualApproval = true;
                    }
                }
            }
        }

        if (sodRuleRepository != null) {
            List<SodRule> sodRules = sodRuleRepository.findByEnabledTrue();
            for (AccessGrantRequest access : accessRequests) {
                String app = normalize(access.getApplication());
                String lvl = normalize(access.getAccessLevel());
                for (SodRule sod : sodRules) {
                    boolean appMatch = normalize(sod.getApp1()).equals(app) || normalize(sod.getApp2()).equals(app);
                    if (appMatch) {
                        if (department.contains("engineering") && (role.contains("lead") || role.contains("manager"))) {
                            score += 45;
                            violations.add(sod.getDescription());
                        }
                    }
                }
            }
        }

        for (AccessGrantRequest access : accessRequests) {
            String application = normalize(access.getApplication());
            String accessLevel = normalize(access.getAccessLevel());

            if (application.isBlank() || accessLevel.isBlank()) {
                violations.add("Every access request must include an application and access level.");
                score += 30;
                continue;
            }

            if (isPrivileged(accessLevel)) {
                score += 35;
                violations.add("Privileged " + accessLevel + " access requested for " + application + ".");
            }

            if (isProductionScope(application, accessLevel)) {
                score += 25;
                violations.add("Production-scoped access requested for " + application + ".");
            }

            if (isSensitiveApplication(application)) {
                score += 15;
                violations.add("Sensitive application access requested for " + application + ".");
            }

            if (isSeparationOfDutiesViolation(department, role, application, accessLevel)) {
                score += 45;
                violations.add("Potential separation-of-duties violation: " + department
                        + " role requested " + accessLevel + " access to " + application + ".");
            }
        }

        score = Math.min(score, 100);
        RiskLevel riskLevel = riskLevel(score);

        SecurityEvaluation evaluation = new SecurityEvaluation();
        evaluation.setRiskScore(score);
        evaluation.setRiskLevel(riskLevel);
        evaluation.setViolations(violations);
        evaluation.setRequiresApproval(forceApproval || riskLevel != RiskLevel.LOW);
        evaluation.setRequiresDualApproval(forceDualApproval || riskLevel == RiskLevel.CRITICAL);
        evaluation.setDecision(evaluation.isRequiresDualApproval() ? "BLOCKED_PENDING_DUAL_APPROVAL"
                : evaluation.isRequiresApproval() ? "PENDING_APPROVAL" : "ALLOWED");
        return evaluation;
    }

    private List<AccessGrantRequest> accessRequests(OnboardRequest request) {
        if (request.getAccessRequests() != null && !request.getAccessRequests().isEmpty()) {
            return request.getAccessRequests();
        }
        if (!isBlank(request.getApplication()) || !isBlank(request.getAccessLevel())) {
            return List.of(new AccessGrantRequest(request.getApplication(), request.getAccessLevel()));
        }
        return List.of();
    }

    private boolean isPrivileged(String accessLevel) {
        return accessLevel.contains("admin")
                || accessLevel.contains("root")
                || accessLevel.contains("owner")
                || accessLevel.contains("superuser")
                || accessLevel.contains("superadmin");
    }

    private boolean isProductionScope(String application, String accessLevel) {
        return application.contains("production")
                || application.contains("prod")
                || accessLevel.contains("production");
    }

    private boolean isSensitiveApplication(String application) {
        return application.contains("aws")
                || application.contains("finance")
                || application.contains("payroll")
                || application.contains("production");
    }

    private boolean isSeparationOfDutiesViolation(String department,
                                                   String role,
                                                   String application,
                                                   String accessLevel) {
        boolean privileged = isPrivileged(accessLevel);
        boolean elevatedRole = role.contains("lead") || role.contains("manager") || role.contains("admin");
        boolean sensitiveTarget = application.contains("finance")
                || application.contains("payroll")
                || application.contains("production");
        return department.contains("engineering") && elevatedRole && privileged && sensitiveTarget;
    }

    private RiskLevel riskLevel(int score) {
        if (score >= 90) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 50) {
            return RiskLevel.HIGH;
        }
        if (score >= 30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
