package compensation_engine.service;

import compensation_engine.model.UserAccount;
import compensation_engine.repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public UserAccount createUser(String username, String rawPassword, String fullName, String email, String role) {
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        String id = "usr-" + UUID.randomUUID().toString().substring(0, 8);
        String hash = passwordEncoder.encode(rawPassword);
        UserAccount account = new UserAccount(id, username, hash, fullName, email, role, true);
        return userAccountRepository.save(account);
    }

    @Transactional
    public UserAccount updateUserRole(String id, String newRole) {
        UserAccount account = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        account.setRole(newRole);
        return userAccountRepository.save(account);
    }

    @Transactional
    public void changePassword(String username, String newRawPassword) {
        UserAccount account = findByUsername(username);
        account.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userAccountRepository.save(account);
    }
}
