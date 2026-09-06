package compensation_engine.agent.reconciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationReport {

    private Instant scannedAt;
    private int employeesScanned;
    private int findingCount;
    private List<DriftFinding> findings = new ArrayList<>();

    public Instant getScannedAt() { return scannedAt; }
    public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }

    public int getEmployeesScanned() { return employeesScanned; }
    public void setEmployeesScanned(int employeesScanned) { this.employeesScanned = employeesScanned; }

    public int getFindingCount() { return findingCount; }
    public void setFindingCount(int findingCount) { this.findingCount = findingCount; }

    public List<DriftFinding> getFindings() { return findings; }
    public void setFindings(List<DriftFinding> findings) {
        this.findings = findings != null ? new ArrayList<>(findings) : new ArrayList<>();
        this.findingCount = this.findings.size();
    }
}
