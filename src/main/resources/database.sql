-- For gen_random_uuid() function
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- User Table
CREATE TABLE users  (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Optimization Purposes
CREATE INDEX idx_users_email ON users(email);

-- User Roles Table
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role),
    CHECK (role IN ('ADMIN', 'SCRUM', 'DEVELOPER', 'END_USER'))
);

-- Task Table
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status TEXT NOT NULL,
    priority INTEGER NOT NULL,
    due_date DATE,
    assigned_user_id UUID,
    reporter_user_id UUID,

    -- Foreign key constraints
    FOREIGN KEY (assigned_user_id) REFERENCES users (id),
    FOREIGN KEY (reporter_user_id) REFERENCES users (id),

    -- Unique constraint on specified columns
    UNIQUE (title, description, status, priority, due_date, assigned_user_id, reporter_user_id),

    -- Check constraint for valid status values
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);


-- Maybe Needed?
CREATE INDEX idx_tasks_assigned_user_id ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_reporter_user_id ON tasks(reporter_user_id);

-- History Table
CREATE TABLE history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    change_description TEXT,
    changed_by UUID NOT NULL,
    change_date TIMESTAMP NOT NULL,

    -- Foreign key constraints
    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE CASCADE
);


-- Notification Table
CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    notification_date TIMESTAMP,
    related_task_id UUID,

    -- Foreign key constraints
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (related_task_id) REFERENCES tasks (id) ON DELETE SET NULL
);
