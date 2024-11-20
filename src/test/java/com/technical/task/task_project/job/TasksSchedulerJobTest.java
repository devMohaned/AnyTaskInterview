package com.technical.task.task_project.job;

import com.technical.task.task_project.model.User;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.EmailService;
import com.technical.task.task_project.service.TaskService;
import com.technical.task.task_project.service.UserService;
import com.technical.task.task_project.service.dto.TaskDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
public class TasksSchedulerJobTest {

    @SpyBean
    private UserService userService;

    @SpyBean
    private UserRepository userRepository;

    @SpyBean
    private EmailService emailService;

    @SpyBean
    private TaskService taskService;

    @SpyBean
    private TasksScheduler tasksScheduler;


    @Test
    public void testSendTaskDeadlineReminders_Success() {

        TaskDTO task1 = TaskDTO.builder()
                .title("Task 1")
                .dueDate(LocalDate.now().plusDays(1)) // Due tomorrow
                .assignedUserId(UUID.randomUUID().toString())
                .build();

        when(taskService.getTasksOrderedByPriorityAndDueDate()).thenReturn(List.of(task1));
        User assignedUser = User.builder().email("testuser@example.com").build();
        Mockito.doReturn(Optional.of(assignedUser)).when(userRepository).findById(Mockito.any());
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());


        tasksScheduler.sendTaskDeadlineReminders();


        verify(emailService, times(1)).sendEmail(eq("testuser@example.com"), anyString(), anyString());
        verify(taskService, times(1)).getTasksOrderedByPriorityAndDueDate();
    }

    @Test
    public void testSendTaskDeadlineReminders_NoTasksToNotify() {

        int allowedDurationBeforeSendingEmail = 3;
        TaskDTO task1 = TaskDTO.builder()
                .title("Task 1")
                .dueDate(LocalDate.now().plusDays(allowedDurationBeforeSendingEmail + 5))
                .assignedUserId(UUID.randomUUID().toString())
                .build();

        when(taskService.getTasksOrderedByPriorityAndDueDate()).thenReturn(List.of(task1));
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());


        tasksScheduler.sendTaskDeadlineReminders();


        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(taskService, times(1)).getTasksOrderedByPriorityAndDueDate();
    }

    @Test
    public void testSendTaskDeadlineReminders_ExceptionHandling() {

        TaskDTO task1 = TaskDTO.builder()
                .title("Task 1")
                .dueDate(LocalDate.now().plusDays(1))
                .assignedUserId(UUID.randomUUID().toString())
                .build();

        when(taskService.getTasksOrderedByPriorityAndDueDate()).thenReturn(List.of(task1));
        Mockito.doThrow(new RuntimeException("User not found")).when(userService).validateUserExistence(Mockito.anyString());
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());


        tasksScheduler.sendTaskDeadlineReminders();


        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString()); // No email should be sent due to exception
        verify(taskService, times(1)).getTasksOrderedByPriorityAndDueDate();
    }
}
