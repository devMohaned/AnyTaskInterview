package com.technical.task.task_project.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE")
    private String status;

    @PositiveOrZero
    private int priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @NotBlank
    private String assignedUserId;
    @NotBlank
    private String reporterUserId;

    private boolean shouldNotifyUser;
}
