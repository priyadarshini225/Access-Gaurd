package compensation_engine.repository;

import compensation_engine.model.SodRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SodRuleRepository extends JpaRepository<SodRule, String> {
    List<SodRule> findByEnabledTrue();
}
