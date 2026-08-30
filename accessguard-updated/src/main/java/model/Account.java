package compensation_engine.model;

public class Account {

    private String accountId;
    private String employeeId;
    private String username;
    private String status;

    public Account() {
    }

    public Account(String accountId, String employeeId,
                   String username, String status) {
        this.accountId = accountId;
        this.employeeId = employeeId;
        this.username = username;
        this.status = status;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}