-- ============================================================
-- V4: Persistent Users, Roles, Access Policies & SoD Rules
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS access_policies (
    id VARCHAR(64) PRIMARY KEY,
    department VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL,
    application VARCHAR(64) NOT NULL,
    max_access_level VARCHAR(64) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    requires_dual_approval BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sod_rules (
    id VARCHAR(64) PRIMARY KEY,
    app1 VARCHAR(64) NOT NULL,
    role1 VARCHAR(64) NOT NULL,
    app2 VARCHAR(64) NOT NULL,
    role2 VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Seed Baseline System Users (BCrypt passwords for admin123, approver123, auditor123, user123)
INSERT INTO users (id, username, password_hash, full_name, email, role, enabled) VALUES
('usr-1', 'admin', '$2a$10$7R0wUqfL.Vn6F1YvP0/zOuO98BfK65o45Fq9eR6h1pP5gq5Gz0d5S', 'System Administrator', 'admin@accessguard.io', 'ADMIN', TRUE),
('usr-2', 'approver', '$2a$10$7R0wUqfL.Vn6F1YvP0/zOuO98BfK65o45Fq9eR6h1pP5gq5Gz0d5S', 'Chief Security Approver', 'approver@accessguard.io', 'APPROVER', TRUE),
('usr-3', 'auditor', '$2a$10$7R0wUqfL.Vn6F1YvP0/zOuO98BfK65o45Fq9eR6h1pP5gq5Gz0d5S', 'Compliance Auditor', 'auditor@accessguard.io', 'SECURITY_AUDITOR', TRUE),
('usr-4', 'user', '$2a$10$7R0wUqfL.Vn6F1YvP0/zOuO98BfK65o45Fq9eR6h1pP5gq5Gz0d5S', 'Standard User', 'user@accessguard.io', 'USER', TRUE);

-- Seed Baseline Access Policies
INSERT INTO access_policies (id, department, role, application, max_access_level, requires_approval, requires_dual_approval, enabled) VALUES
('pol-1', 'Engineering', '*', 'AWS', 'Admin', TRUE, FALSE, TRUE),
('pol-2', 'Finance', '*', 'Production Finance', 'Superadmin', TRUE, TRUE, TRUE),
('pol-3', '*', '*', 'Production Finance', 'Superadmin', TRUE, TRUE, TRUE);

-- Seed Baseline Separation of Duties (SoD) Conflict Rules
INSERT INTO sod_rules (id, app1, role1, app2, role2, description, risk_level, enabled) VALUES
('sod-1', 'Production Finance', 'Superadmin', 'Engineering Lead', '*', 'Conflicting separation-of-duties access: Production Finance Superadmin held by Engineering Lead role.', 'CRITICAL', TRUE),
('sod-2', 'Finance', 'Admin', 'Production Finance', 'Superadmin', 'Conflicting separation-of-duties access: Finance Admin combined with Production Finance Superadmin.', 'CRITICAL', TRUE);
