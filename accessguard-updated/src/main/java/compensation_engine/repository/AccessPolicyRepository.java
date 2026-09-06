package compensation_engine.repository;

import compensation_engine.model.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, String> {
    List<AccessPolicy> findByEnabledTrue();
}
