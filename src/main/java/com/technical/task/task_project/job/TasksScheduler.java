package com.technical.task.task_project.job;


import com.technical.task.task_project.model.User;
import com.technical.task.task_project.service.EmailService;
import com.technical.task.task_project.service.TaskService;
import com.technical.task.task_project.service.UserService;
import com.technical.task.task_project.service.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;


@Component
@RequiredArgsConstructor
@Log4j2
public class TasksScheduler {

    @Value("${task.notification.buffered-days}")
    private long allowedDurationBeforeSendingEmail;

    private final UserService userService;
    private final EmailService emailService;
    private final TaskService taskService;

    // Send email every day at 12 AM (This should be different microservice, for simplicity, including it here aswell)
    // * * * * * ? = Too frequently
    // 0 0 * * * ? = 12 AM everyday
    @Scheduled(cron = "0 0 * * * ?")
    public void sendTaskDeadlineReminders() {

        log.info("Cron Job started at [{}]", LocalDateTime.now());
        List<TaskDTO> tasksOrderedByPriorityAndDueDate = taskService.getTasksOrderedByPriorityAndDueDate();
        for (TaskDTO task : tasksOrderedByPriorityAndDueDate) {
            LocalDate currentDate = LocalDate.now();
            long remainingToCompleteTaskInDays = Math.abs(Duration.ofDays(DAYS.between(task.getDueDate(), currentDate)).toDays());
            boolean needsNotificationCauseUrgent = remainingToCompleteTaskInDays < allowedDurationBeforeSendingEmail;

            if (needsNotificationCauseUrgent) {
                String subject = String.format("Task [%s] Deadline Reminder", task.getTitle());
                String body = String.format("This is a reminder for your upcoming task deadline. Task Deadline [%s], Remaining Days[%s]", task.getDueDate(), remainingToCompleteTaskInDays);
                try {
                    User assignedUser = userService.validateUserExistence(UUID.fromString(task.getAssignedUserId()));
                    emailService.sendEmail(assignedUser.getEmail(), subject, body);
                } catch (Exception e) {
                    log.error("Unexpected Error happened while notifying users for their late tasks. Error: [{}]", e.getMessage(), e);
                }
            }
        }

    }
}
