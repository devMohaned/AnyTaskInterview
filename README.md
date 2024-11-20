# Task Management Application
How to (Video):
https://youtu.be/Ye5Z7AEoYZY

Coverage:
![Coverage](https://github.com/user-attachments/assets/b04a6a6d-1497-4bd6-bfef-5206eaa3f5eb)

## Prerequisites

Before starting, ensure you have the following tools installed:

- Postman
- Docker
- Database Tool (e.g., PgAdmin, DBeaver)
- Java 17 JDK
- IDE (e.g., IntelliJ, Eclipse) to view and modify the code
- Maven

> **IMPORTANT**: You will need a Database Tool to access the database and view IDs required for other APIs.

---

## Getting Started

### 1. View APIs via Swagger
1. Open [Swagger Editor](https://editor.swagger.io/).
2. Navigate to `task_project/resources/swagger.yaml`.
3. Copy the content and paste it into Swagger Editor.
4. View the available APIs.

---

### 2. Docker (Optional: Use if you don’t have a local PostgreSQL database)
1. Install Docker.
2. Download the PostgreSQL image from [Docker Hub](https://hub.docker.com/_/postgres) (use tag `12`).
3. Create a container with your credentials:
   - Database Name, Username, Password, and Port.
4. Run the container on the specified port.

---

### 3. Postman
1. Install Postman and register if required.
2. Import the Postman collection:
   - Navigate to `task_project/resources/Task API.postman_collection.json`.
   - Copy its content and import it into Postman.
3. Explore the folders:
   - **Users Folder**: Authentication and user-related APIs.
   - **Tasks Folder**: Task management APIs.
4. Modify requests and test APIs. 
   > Note: Retrieve IDs from the database after insertion to use other APIs.

#### APIs That Don't Require JWT Tokens:
- **Register User**: `POST /api/users/register`
- **Authenticate**: `POST /authenticate` (returns the JWT token)

#### All Other APIs:
- Require a valid JWT token.
- Many APIs require specific roles.

---

### 4. IDE Setup
1. Clone the project to your local directory.
2. Install Maven.
3. Open the project in your preferred Java IDE.
4. Configure `application.yaml` in `task_project/resources`:
   - **Mail Configuration**:
     - Update email credentials (`username` and `password`).
   - **Database Configuration**:
     - Update the `url`, `username`, and `password` with your database details.
   - **Optional Configurations**:
     - JWT secret-key, time-to-live (TTL), rate limiting, notification settings.
5. Populate the database:
   - Use your database tool or terminal to execute `data.sql`.
6. Update cron job timing in `TasksScheduler.java` for testing purposes.

---

## Running the Project
1. Start the Spring Boot application.
2. Use Postman to test API functionality.
3. Verify data insertion and retrieval from the database.

---

## Usage Examples

### **Creating Users**
Sample request bodies for various user roles:

#### Admin User:
{
  "name": "Developer User",
  "email": "dev@example.com",
  "password": "hashedPassword2",
  "roles": ["DEVELOPER"]
}


Creating Tasks
Sample request bodies for various tasks:

Task Assigned to Developer:
{
  "title": "Task 1",
  "description": "Description for Task 1",
  "status": "TODO",
  "priority": 1,
  "dueDate": "2024-01-10",
  "assignedUserId": "uuid-of-developer-user",
  "reporterUserId": "uuid-of-admin-user",
  "shouldNotifyUser": false
}


Task History
Examples of task history events:

Task Created:
{
  "taskId": "uuid-of-task-1",
  "changeDescription": "Task Created",
  "changedBy": "uuid-of-admin-user",
  "changeDate": "2024-01-01T10:00:00"
}


Status Changed:
{
  "taskId": "uuid-of-task-2",
  "changeDescription": "Status changed to IN_PROGRESS",
  "changedBy": "uuid-of-scrum-master",
  "changeDate": "2024-01-02T12:00:00"
}




Notifications
Examples of notifications:

Task Assigned:
{
  "userId": "uuid-of-developer-user",
  "message": "You have been assigned to Task 1",
  "isRead": false,
  "notificationDate": "2024-01-01T10:15:00",
  "relatedTaskId": "uuid-of-task-1"
}
Task Reminder:
{
  "userId": "uuid-of-admin-user",
  "message": "Reminder: Task 5 due soon",
  "isRead": true,
  "notificationDate": "2024-01-03T09:00:00",
  "relatedTaskId": "uuid-of-task-5"
}
