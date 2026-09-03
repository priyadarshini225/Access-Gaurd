package compensation_engine.repository;

import compensation_engine.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /**
     * Find all employees with an exact case-insensitive name match.
     * Used to resolve offboarding by name without relying on
     * the makeId() heuristic.
     */
    List<Employee> findByNameIgnoreCase(String name);

    /**
     * Find all employees that are currently active.
     */
    List<Employee> findByStatus(String status);
}
