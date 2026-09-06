package compensation_engine.controller;

import compensation_engine.model.AccessPolicy;
import compensation_engine.model.SodRule;
import compensation_engine.repository.AccessPolicyRepository;
import compensation_engine.repository.SodRuleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final AccessPolicyRepository accessPolicyRepository;
    private final SodRuleRepository sodRuleRepository;

    public PolicyController(AccessPolicyRepository accessPolicyRepository, SodRuleRepository sodRuleRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
        this.sodRuleRepository = sodRuleRepository;
    }

    @GetMapping
    public List<AccessPolicy> getPolicies() {
        return accessPolicyRepository.findAll();
    }

    @PostMapping
    public AccessPolicy createPolicy(@RequestBody AccessPolicy policy) {
        if (policy.getId() == null || policy.getId().isBlank()) {
            policy.setId("pol-" + System.currentTimeMillis());
        }
        return accessPolicyRepository.save(policy);
    }

    @GetMapping("/sod-rules")
    public List<SodRule> getSodRules() {
        return sodRuleRepository.findAll();
    }

    @PostMapping("/sod-rules")
    public SodRule createSodRule(@RequestBody SodRule sodRule) {
        if (sodRule.getId() == null || sodRule.getId().isBlank()) {
            sodRule.setId("sod-" + System.currentTimeMillis());
        }
        return sodRuleRepository.save(sodRule);
    }
}
