package com.technical.task.task_project.service;


import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Status;
import com.technical.task.task_project.repository.TaskRepository;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.dto.TaskDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("junit")
public class TaskSearchFilterationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User assignedUser1;
    private User assignedUser2;
    private User reporterUser1;
    private User reporterUser2;

    @BeforeEach
    public void setUp() {
        assignedUser1 = User.builder()
                .id(UUID.randomUUID())
                .name("Assigned User 1")
                .email("assigned1@example.com")
                .password("Password1")
                .build();
        assignedUser1 = userRepository.save(assignedUser1);

        assignedUser2 = User.builder()
                .id(UUID.randomUUID())
                .name("Assigned User 2")
                .password("Password2")
                .email("assigned2@example.com")
                .build();
        assignedUser2 = userRepository.save(assignedUser2);

        reporterUser1 = User.builder()
                .id(UUID.randomUUID())
                .name("Reporter User 1")
                .password("Password1")
                .email("reporter1@example.com")
                .build();
        reporterUser1 = userRepository.save(reporterUser1);

        reporterUser2 = User.builder()
                .id(UUID.randomUUID())
                .name("Reporter User 2")
                .password("Password2")
                .email("reporter2@example.com")
                .build();
        reporterUser2 = userRepository.save(reporterUser2);

        taskRepository.save(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 1")
                .description("Description 1")
                .priority(1)
                .status(Status.TODO)
                .dueDate(LocalDate.now().plusDays(5))
                .assignedUser(assignedUser1)
                .reporterUser(reporterUser1)
                .build());

        taskRepository.save(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 2")
                .description("Description 2")
                .priority(2)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(10))
                .assignedUser(assignedUser2)
                .reporterUser(reporterUser2)
                .build());

        taskRepository.save(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 3")
                .description("Description 3")
                .priority(3)
                .status(Status.DONE)
                .dueDate(LocalDate.now().plusDays(15))
                .assignedUser(assignedUser1)
                .reporterUser(reporterUser2)
                .build());

        taskRepository.save(Task.builder()
                .id(UUID.randomUUID())
                .title("Special Task")
                .description("Special Description")
                .priority(4)
                .status(Status.TODO)
                .dueDate(LocalDate.now().minusDays(2))
                .assignedUser(assignedUser2)
                .reporterUser(reporterUser1)
                .build());


        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("assigned1@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    private void teardown() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSearchAndFilterTasks_ByTitle() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks("Task 1", null, null, null, null, null, null, null, null, 0, 10);
        assertEquals(1, result.getTotalElements());
        assertEquals("Task 1", result.getContent().get(0).getTitle());
    }

    @Test
    public void testSearchAndFilterTasks_ByStatus() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks(null, null, Status.TODO, null, null, null, null, null, null, 0, 10);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(task -> task.getStatus().equals("TODO")));
    }

    @Test
    public void testSearchAndFilterTasks_ByPriorityRange() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks(null, null, null, 2, 4, null, null, null, null, 0, 10);
        assertEquals(3, result.getTotalElements());
    }

    @Test
    public void testSearchAndFilterTasks_ByAssignedUser() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks(null, null, null, null, null, assignedUser1.getId(), null, null, null, 0, 10);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(task -> task.getAssignedUserId().equals(assignedUser1.getId().toString())));
    }

    @Test
    public void testSearchAndFilterTasks_ByReporterUser() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks(null, null, null, null, null, null, reporterUser2.getId(), null, null, 0, 10);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    public void testSearchAndFilterTasks_ByDueDateRange() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks(null, null, null, null, null, null, null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), 0, 10);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    public void testSearchAndFilterTasks_CombinedFilters() {
        Page<TaskDTO> result = taskService.searchAndFilterTasks("Special Task", "Special Description", Status.TODO, null, null, assignedUser2.getId(), reporterUser1.getId(), null, null, 0, 10);
        assertEquals(1, result.getTotalElements());
        assertEquals("Special Task", result.getContent().get(0).getTitle());
    }
}
