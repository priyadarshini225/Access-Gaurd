# 🛡 AccessGuard Compensation Engine

**AccessGuard** is an Agentic Employee Access Management and Transaction Compensation Engine built with **Java 21**, **Spring Boot 3.4.5**, **Spring Data JPA**, and **Ollama (Qwen2.5 1.5B)**.

It provides a distributed transaction mechanism (using the **Saga Pattern**) to ensure that complex multi-step identity provisioning operations can be safely rolled back in reverse order when a step fails, while strictly enforcing security policies that prevent dangerous automatic restoration of revoked access during offboarding.

---

## 🌟 Architecture & Core Principles

### End-to-End Processing Pipeline

```
Natural Language Request ("Onboard Kavya as Developer and give GitLab access")
               │
               ▼
   [ Ollama AI Service ]  ── (Qwen2.5 1.5B via Jackson JSON Deserialization)
               │
               ▼
     [ Validated DTO ]    ── (Strong typing, Bean Validation constraints)
               │
               ▼
   [ Workflow Service ]   ── (Intent mapping & Employee resolution)
               │
               ▼
     [ Saga Engine ]      ── (Step-by-step forward execution)
               │
      ┌────────┴────────┐
      ▼                 ▼
   SUCCESS          FAILURE
   (Persist)           │
                       ▼
             [ Workflow Policy Check ]
             ├── ROLLBACK_ON_FAILURE  (Onboarding: Compensates previous steps in REVERSE order)
             └── KEEP_PARTIAL_COMPLETION (Offboarding: Preserves revoked access; never restores access)
```

---

## 🔒 Safety Design: Why Offboarding Differs from Onboarding

| Operation | Workflow Policy | Failure Behavior | Rationale |
|---|---|---|---|
| **Onboarding / Provisioning** | `ROLLBACK_ON_FAILURE` | Rolls back already created resources in **reverse order** (Storage → Access → Email → Account → Employee). | If provisioning fails halfway, leaving orphaned active accounts, unlinked email addresses, or unmonitored storage creates severe security vulnerabilities. |
| **Offboarding / Revocation** | `KEEP_PARTIAL_COMPLETION` | **Halts execution and preserves partial state**. Does **NOT** execute compensation to re-enable accounts or grant back application access. | If offboarding fails (e.g., HR database times out after revoking GitLab access), automatically "compensating" would grant back corporate access to a terminated employee. |

---

## 🚀 Prerequisites & Installation Checklist

### 1. Java Development Kit (JDK 21+)
* **Requirement**: JDK 21 (or newer, e.g., JDK 23)
* **Verify installation**:
  ```powershell
  java -version
  javac -version
  ```

### 2. Apache Maven (3.9+)
* **Requirement**: Maven 3.9.x
* **Installation** (Windows):
  ```powershell
  winget install Apache.Maven
  # or download from https://maven.apache.org/download.cgi and add bin/ to PATH
  ```
* **Verify installation**:
  ```powershell
  mvn -version
  ```

### 3. Ollama (AI Intent Parsing)
* **Requirement**: Local Ollama runtime with `qwen2.5:1.5b`
* **Installation**:
  ```powershell
  winget install Ollama.Ollama
  # or download from https://ollama.com
  ```
* **Start Ollama service**:
  ```powershell
  ollama serve
  ```
* **Pull the model**:
  ```powershell
  ollama pull qwen2.5:1.5b
  ```
* **Verify model**:
  ```powershell
  ollama list
  ```

---

## 💾 Database Configuration & Persistence

AccessGuard uses **H2 in file mode** by default for zero-friction local development, persisting data to `./data/accessguard.mv.db` across server restarts.

### Database Web Console (H2)
* **URL**: `http://localhost:8080/h2-console`
* **JDBC URL**: `jdbc:h2:file:./data/accessguard`
* **Username**: `sa`
* **Password**: *(leave empty)*

### Database support
AccessGuard uses H2 file mode as its supported database:

```text
JDBC URL: jdbc:h2:file:./data/accessguard
Username: sa
Password: empty
```

No PostgreSQL installation or database setup is required.

---

## 🏃 Running the Application

### 1. Build and Run via Maven
```powershell
cd accessguard-updated
mvn spring-boot:run
```

Authentication is enabled by default. The dashboard accepts the seeded `admin`
account credentials through its sign-in controls. For an unsecured local demo
only, run with the development profile:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For local development, the built-in role accounts are:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | Full administrator |
| `operator` | `operator123` | Workflow operator |
| `approver` | `approver123` | Approval decisions |
| `auditor` | `auditor123` | Audit and reconciliation read access |
| `user` | `user123` | Standard dashboard user |

These are development credentials. Override them with `security.users.*.password`
properties before using the application outside local development. Self-signup
creates standard `USER` accounts only.

### 2. Run Automated Test Suite
```powershell
mvn test
```

---

## 📡 REST API Documentation

### 1. Health Check
* **Endpoint**: `GET /api/compensation/health`
* **Response**:
  ```json
  {
    "status": "OK",
    "message": "AccessGuard compensation engine is running"
  }
  ```

---

### 2. AI Plan (Preview Only)
* **Endpoint**: `POST /api/ai/plan`
* **Request**:
  ```json
  {
    "message": "Onboard Kavya Sharma from Engineering as Senior Architect and give GitLab Maintainer access"
  }
  ```
* **Response**:
  ```json
  {
    "intent": "ONBOARD",
    "name": "Kavya Sharma",
    "employeeId": "",
    "department": "Engineering",
    "role": "Senior Architect",
    "application": "GitLab",
    "accessLevel": "Maintainer",
    "aiModel": "qwen2.5:1.5b",
    "aiStatus": "ONLINE"
  }
  ```

---

### 3. End-to-End AI Execution
* **Endpoint**: `POST /api/ai/execute`
* **Request**:
  ```json
  {
    "message": "Onboard Alice Smith from Security as Lead and grant AWS Admin access"
  }
  ```
* **Response**:
  ```json
  {
    "workflowId": "a9310e54-5264-4e94-a131-7bc924976722",
    "workflowType": "ONBOARD",
    "employeeId": "alicesmith",
    "status": "SUCCESS",
    "message": "All workflow steps completed successfully.",
    "durationMs": 42,
    "steps": [
      { "name": "Create Employee", "status": "SUCCESS" },
      { "name": "Create Account", "status": "SUCCESS" },
      { "name": "Create Email", "status": "SUCCESS" },
      { "name": "Grant AWS Access", "status": "SUCCESS" },
      { "name": "Create Storage", "status": "SUCCESS" }
    ],
    "compensation": []
  }
  ```

---

### 4. Direct Workflow Onboarding
* **Endpoint**: `POST /api/workflow/onboard`
* **Request**:
  ```json
  {
    "name": "Kavya Sharma",
    "employeeId": "kavya-001",
    "department": "Engineering",
    "role": "Lead Architect",
    "application": "GitLab",
    "accessLevel": "Maintainer"
  }
  ```

---

### 5. Application Access Revocation (Granular)
*Revokes only the specified app access without disabling the account or deactivating the employee.*
* **Endpoint**: `POST /api/workflow/revoke-access`
* **Request**:
  ```json
  {
    "employeeId": "kavya-001",
    "application": "GitLab"
  }
  ```

---

### 6. Full Employee Offboarding (by Name or ID)
*Supports smart name resolution. If multiple employees share the name, returns 409 Conflict with matching IDs.*
* **Endpoint**: `POST /api/workflow/offboard`
* **Request**:
  ```json
  {
    "name": "Kavya Sharma"
  }
  ```

---

### 7. State & Execution History Inspection
* **Get Current State**: `GET /api/workflow/state`
* **Get Workflow Execution Logs**: `GET /api/workflow/history`

---

### 8. Compensation & Failure Testing
* **Test Onboarding Failure Rollback**:
  * `POST /api/compensation/test/onboarding-failure`
  * Body: `{"name":"Bob","employeeId":"bob-01","department":"HR","role":"Lead","application":"Slack","accessLevel":"User","failAt":"Create Storage"}`
  * Result: Steps 1-4 execute, Step 5 fails, Steps 4, 3, 2, 1 are compensated in reverse order. Status: `ROLLED_BACK`.
* **Test Offboarding Partial Preservation**:
  * `POST /api/compensation/test/offboarding-failure`
  * Body: `{"employeeId":"bob-01","application":"Slack","failAt":"Revoke Resources"}`
  * Result: Revocation steps prior to failure are preserved, no automatic re-granting occurs. Status: `PARTIAL`.

---

## 📂 Project Structure

```
accessguard-updated/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/compensation_engine/
    │   │   ├── CompensationEngineApplication.java
    │   │   ├── ai/
    │   │   │   └── OllamaService.java
    │   │   ├── controller/
    │   │   │   ├── AiController.java
    │   │   │   ├── CompensationController.java
    │   │   │   └── WorkflowController.java
    │   │   ├── dto/
    │   │   │   ├── AiExecuteRequest.java
    │   │   │   ├── AiPlanRequest.java
    │   │   │   ├── AiPlanResponse.java
    │   │   │   ├── OffboardRequest.java
    │   │   │   ├── OnboardRequest.java
    │   │   │   └── RevokeAccessRequest.java
    │   │   ├── exception/
    │   │   │   ├── AmbiguousEmployeeNameException.java
    │   │   │   ├── EmployeeAlreadyExistsException.java
    │   │   │   ├── EmployeeNotFoundException.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── OllamaUnavailableException.java
    │   │   ├── model/
    │   │   │   ├── Account.java
    │   │   │   ├── ApplicationAccess.java
    │   │   │   ├── Email.java
    │   │   │   ├── Employee.java
    │   │   │   ├── Resource.java
    │   │   │   └── WorkflowExecution.java
    │   │   ├── repository/
    │   │   │   ├── AccountRepository.java
    │   │   │   ├── ApplicationAccessRepository.java
    │   │   │   ├── EmailRepository.java
    │   │   │   ├── EmployeeRepository.java
    │   │   │   ├── ResourceRepository.java
    │   │   │   └── WorkflowExecutionRepository.java
    │   │   ├── saga/
    │   │   │   ├── Compensator.java
    │   │   │   ├── Saga.java
    │   │   │   ├── SagaResult.java
    │   │   │   ├── SagaStep.java
    │   │   │   └── WorkflowPolicy.java
    │   │   ├── service/
    │   │   │   ├── AccessService.java
    │   │   │   ├── AccountService.java
    │   │   │   ├── EmailService.java
    │   │   │   ├── EmployeeService.java
    │   │   │   ├── ResourceService.java
    │   │   │   ├── WorkflowExecutionService.java
    │   │   │   └── WorkflowService.java
    │   │   └── workflow/
    │   │       ├── AccessRevocationWorkflow.java
    │   │       ├── OffboardingWorkflow.java
    │   │       └── OnboardingWorkflow.java
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    │           └── index.html
    └── test/
        └── java/compensation_engine/
            ├── CompensationEngineApplicationTests.java
            ├── ai/
            │   └── OllamaServiceTest.java
            ├── integration/
            │   └── AccessGuardIntegrationTest.java
            ├── saga/
            │   └── SagaTest.java
            └── service/
                └── EmployeeServiceTest.java
```
