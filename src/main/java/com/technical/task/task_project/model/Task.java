package com.technical.task.task_project.model;

import com.technical.task.task_project.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity(name = "tasks")
@Table(name = "tasks", uniqueConstraints = @UniqueConstraint(columnNames = {
        "title", "description", "status", "priority", "dueDate", "assigned_user_id", "reporter_user_id"
}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int priority;

    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @ManyToOne
    @JoinColumn(name = "reporter_user_id")
    private User reporterUser;

}
