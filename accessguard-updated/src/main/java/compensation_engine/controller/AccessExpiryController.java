package compensation_engine.controller;

import compensation_engine.service.AccessExpiryService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/access-expiry")
@CrossOrigin
public class AccessExpiryController {

    private final AccessExpiryService accessExpiryService;

    public AccessExpiryController(AccessExpiryService accessExpiryService) {
        this.accessExpiryService = accessExpiryService;
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public List<String> process() {
        return accessExpiryService.revokeExpired();
    }
}