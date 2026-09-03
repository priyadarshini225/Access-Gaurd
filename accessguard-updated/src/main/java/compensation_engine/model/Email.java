package compensation_engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emails")
public class Email {

    @Id
    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    public Email() {}

    public Email(String employeeId, String emailAddress) {
        this.employeeId = employeeId;
        this.emailAddress = emailAddress;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
}
