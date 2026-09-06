-- V3 Migration: Add approval_requests, audit_events, external_access_grants, and remediation_tasks tables
CREATE TABLE IF NOT EXISTS approval_requests (
    request_id VARCHAR(255) NOT NULL,
    requester VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255),
    operation VARCHAR(255) NOT NULL,
    request_payload VARCHAR(20000) NOT NULL,
    risk_level VARCHAR(255) NOT NULL,
    risk_score INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    policy_decision VARCHAR(255) NOT NULL,
    violations VARCHAR(4000),
    first_approver VARCHAR(255),
    second_approver VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    workflow_id VARCHAR(255),
    PRIMARY KEY (request_id),
    CONSTRAINT uk_approval_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id VARCHAR(255) NOT NULL,
    request_id VARCHAR(255),
    workflow_id VARCHAR(255),
    actor VARCHAR(255) NOT NULL,
    actor_type VARCHAR(255) NOT NULL,
    agent_name VARCHAR(255),
    event_type VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    decision VARCHAR(255) NOT NULL,
    details VARCHAR(5000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE TABLE IF NOT EXISTS external_access_grants (
    grant_id VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255) NOT NULL,
    application VARCHAR(255) NOT NULL,
    access_level VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (grant_id)
);

CREATE TABLE IF NOT EXISTS remediation_tasks (
    task_id VARCHAR(255) NOT NULL,
    finding_code VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255) NOT NULL,
    action VARCHAR(1000) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    disposition VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    approved_by VARCHAR(255),
    last_error VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (task_id)
);
