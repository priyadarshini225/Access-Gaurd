package compensation_engine.agent.identity;

public class CandidateMatch {

    private String employeeId;
    private String name;
    private String department;
    private String role;
    private String email;
    private String status;

    public CandidateMatch() {}

    public CandidateMatch(String employeeId, String name, String department, String role, String email, String status) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.role = role;
        this.email = email;
        this.status = status;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
