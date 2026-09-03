package compensation_engine.repository;

import compensation_engine.model.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecution, String> {

    List<WorkflowExecution> findByEmployeeIdOrderByExecutedAtDesc(String employeeId);

    List<WorkflowExecution> findTop20ByOrderByExecutedAtDesc();
}
