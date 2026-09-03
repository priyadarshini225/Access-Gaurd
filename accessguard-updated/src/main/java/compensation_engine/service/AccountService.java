package compensation_engine.service;

import compensation_engine.exception.EmployeeNotFoundException;
import compensation_engine.model.Account;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;

    public AccountService(AccountRepository accountRepository,
                          EmployeeRepository employeeRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Account createAccount(String employeeId, String username) {
        validate(employeeId, "Employee ID");
        validate(username, "Username");

        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }

        String accountId = "ACC-" + employeeId;

        if (accountRepository.existsById(accountId)) {
            throw new IllegalStateException(
                    "Account already exists for employee: " + employeeId);
        }

        Account account = new Account(accountId, employeeId, username, "ACTIVE");
        Account saved = accountRepository.save(account);
        log.info("Created account: accountId={}, employeeId={}", accountId, employeeId);
        return saved;
    }

    /**
     * OFFBOARDING PATH — strict.
     * Throws DataInconsistencyException if no account record exists.
     */
    @Transactional
    public void disableAccount(String employeeId) {
        validate(employeeId, "Employee ID");

        List<Account> accounts = accountRepository.findByEmployeeId(employeeId);

        if (accounts.isEmpty()) {
            throw new compensation_engine.exception.DataInconsistencyException("Account", employeeId);
        }

        for (Account account : accounts) {
            if (!"DISABLED".equalsIgnoreCase(account.getStatus())) {
                account.setStatus("DISABLED");
                accountRepository.save(account);
                log.info("Disabled account: {} for employee: {}",
                        account.getAccountId(), employeeId);
            } else {
                log.info("Account {} already DISABLED — skipping.", account.getAccountId());
            }
        }
    }

    /**
     * ADMIN / SABOTAGE PATH.
     * Hard-deletes all account records so disableAccount() will fail during offboarding.
     */
    @Transactional
    public void sabotageAccount(String employeeId) {
        validate(employeeId, "Employee ID");
        List<Account> accounts = accountRepository.findByEmployeeId(employeeId);
        accountRepository.deleteAll(accounts);
        log.warn("ADMIN SABOTAGE: Account records for employee {} force-deleted from database.", employeeId);
    }

    /** Compensation-only: delete account record entirely. */
    @Transactional
    public void deleteAccount(String employeeId) {
        List<Account> accounts = accountRepository.findByEmployeeId(employeeId);
        accountRepository.deleteAll(accounts);
        log.info("Deleted account (compensation): employeeId={}", employeeId);
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
