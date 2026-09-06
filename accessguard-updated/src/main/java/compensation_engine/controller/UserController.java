package compensation_engine.controller;

import compensation_engine.model.UserAccount;
import compensation_engine.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping
    public List<UserAccount> getAllUsers() {
        return userService.findAll();
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String fullName;
        private String email;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    @PostMapping
    public UserAccount createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail(),
                request.getRole() != null ? request.getRole() : "USER"
        );
    }

    @PutMapping("/{id}/role")
    public UserAccount updateRole(@PathVariable String id, @RequestParam String role) {
        return userService.updateUserRole(id, role);
    }
}
