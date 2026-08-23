package compensation_engine.controller;

import compensation_engine.saga.Saga;
import compensation_engine.workflow.ProvisioningWorkflow;
import compensation_engine.workflow.RevocationWorkflow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompensationController {

    @GetMapping("/provision")
    public String provision() {
        try {
            Saga saga = ProvisioningWorkflow.create();
            saga.execute();

            return "Provisioning completed";
        } catch (Exception e) {
            return "Provisioning failed. Rollback completed.";
        }
    }

    @GetMapping("/revoke")
    public String revoke() {
        Saga saga = RevocationWorkflow.create();

        try {
            saga.execute();

            return "Revocation completed";
        } catch (Exception e) {
            return "Revocation partially completed. Residual: "
                    + saga.getResidual();
        }
    }
}