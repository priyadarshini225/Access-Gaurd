package compensation_engine.agent.remediation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RemediationPlan {

    private Instant generatedAt;
    private int taskCount;
    private List<RemediationTask> tasks = new ArrayList<>();

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }

    public List<RemediationTask> getTasks() { return tasks; }
    public void setTasks(List<RemediationTask> tasks) {
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.taskCount = this.tasks.size();
    }
}
