package compensation_engine.repository;

import compensation_engine.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByEmployeeId(String employeeId);

    Optional<Account> findFirstByEmployeeId(String employeeId);
}
