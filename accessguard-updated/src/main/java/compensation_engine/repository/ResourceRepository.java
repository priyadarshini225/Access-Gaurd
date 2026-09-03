package compensation_engine.repository;

import compensation_engine.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {

    List<Resource> findByEmployeeId(String employeeId);

    void deleteByEmployeeId(String employeeId);
}
