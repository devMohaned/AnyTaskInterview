package com.technical.task.task_project.service;

import com.technical.task.task_project.exception.DuplicateException;
import com.technical.task.task_project.exception.NotFoundException;
import com.technical.task.task_project.model.History;
import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Status;
import com.technical.task.task_project.repository.HistoryRepository;
import com.technical.task.task_project.repository.NotificationRepository;
import com.technical.task.task_project.repository.TaskRepository;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.dto.TaskDTO;
import com.technical.task.task_project.service.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @SpyBean
    private EmailService emailService;


    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private TaskRepository taskRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @SpyBean
    private TaskMapper taskMapper;


    private User assignedUser1;
    private User assignedUser2;
    private User reporterUser1;
    private User reporterUser2;
    private Task existingTask;

    @BeforeEach
    public void setUp() {
        assignedUser1 = User.builder()
                .id(UUID.randomUUID())
                .name("Assigned User 1")
                .email("assigned1@example.com")
                .password("Password1")
                .isActive(true)
                .build();
        assignedUser1 = userRepository.save(assignedUser1);

        assignedUser2 = User.builder()
                .id(UUID.randomUUID())
                .name("Assigned User 2")
                .password("Password2")
                .email("assigned2@example.com")
                .isActive(true)
                .build();
        assignedUser2 = userRepository.save(assignedUser2);

        reporterUser1 = User.builder()
                .id(UUID.randomUUID())
                .name("Reporter User 1")
                .password("Password1")
                .email("reporter1@example.com")
                .isActive(true)
                .build();
        reporterUser1 = userRepository.save(reporterUser1);

        reporterUser2 = User.builder()
                .id(UUID.randomUUID())
                .name("Reporter User 2")
                .password("Password2")
                .email("reporter2@example.com")
                .isActive(true)
                .build();
        reporterUser2 = userRepository.save(reporterUser2);

        existingTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Existing Task")
                .description("Existing Description")
                .priority(1)
                .status(Status.TODO)
                .dueDate(LocalDate.now().plusDays(4))
                .assignedUser(assignedUser1)
                .reporterUser(reporterUser1)
                .build();

        existingTask = taskRepository.save(existingTask);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("assigned1@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

    }

    @AfterEach
    private void teardown() {
        historyRepository.deleteAll();
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testCreateMultipleTasks_Success() {
        taskRepository.deleteAll();

        TaskDTO taskDTO1 = TaskDTO.builder()
                .title("Task 1")
                .description("Description 1")
                .priority(1)
                .status("TODO")
                .dueDate(LocalDate.now().plusDays(5))
                .assignedUserId(assignedUser1.getId().toString())
                .reporterUserId(reporterUser1.getId().toString())
                .shouldNotifyUser(true)
                .build();

        TaskDTO taskDTO2 = TaskDTO.builder()
                .title("Task 2")
                .description("Description 2")
                .priority(2)
                .status("IN_PROGRESS")
                .dueDate(LocalDate.now().plusDays(10))
                .assignedUserId(assignedUser2.getId().toString())
                .reporterUserId(reporterUser2.getId().toString())
                .shouldNotifyUser(false)
                .build();

        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());


        TaskDTO result1 = taskService.createTask(taskDTO1);
        TaskDTO result2 = taskService.createTask(taskDTO2);
        assertThrows(DataIntegrityViolationException.class, () -> taskService.createTask(result2));


        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("Task 1", result1.getTitle());
        assertEquals("Task 2", result2.getTitle());
        assertEquals(2, taskRepository.count());
        assertEquals(2, historyRepository.count());
        assertEquals(1, notificationRepository.count());

        TaskDTO taskDTO3 = TaskDTO.builder()
                .title("Task 3")
                .description("Description 3")
                .priority(3)
                .status("IN_PROGRESS")
                .dueDate(LocalDate.now().plusDays(12))
                .assignedUserId(UUID.randomUUID().toString())
                .reporterUserId(reporterUser2.getId().toString())
                .shouldNotifyUser(false)
                .build();

        assertThrows(NotFoundException.class, () -> taskService.createTask(taskDTO3));
        Mockito.doThrow(new DataIntegrityViolationException("Duplicate Error")).when(taskRepository).save(Mockito.any(Task.class));
        assertThrows(DuplicateException.class, () -> taskService.createTask(taskDTO1));

    }

    @Test
    public void testUpdateTask_AssignedToDifferentUser() {

        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Task to Update")
                .description("Original Description")
                .priority(1)
                .status(Status.TODO)
                .dueDate(LocalDate.now().plusDays(3))
                .assignedUser(assignedUser1)
                .reporterUser(reporterUser1)
                .build();
        task = taskRepository.save(task);

        TaskDTO taskDTO = TaskDTO.builder()
                .title("Updated Task")
                .description("Updated Description")
                .priority(1)
                .status("TODO")
                .dueDate(LocalDate.now().plusDays(3))
                .assignedUserId(assignedUser1.getId().toString())
                .reporterUserId(reporterUser1.getId().toString())
                .shouldNotifyUser(true)
                .build();

        historyRepository.save(History.builder().id(UUID.randomUUID()).changedBy(assignedUser1).task(task).changeDescription("Task was created").changeDate(LocalDateTime.now()).build());
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());


        TaskDTO result = taskService.updateTask(task.getId(), taskDTO);


        assertNotNull(result);
        assertEquals("Updated Task", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(2, historyRepository.count());
        assertEquals(1, notificationRepository.count());
    }

    @Test
    public void testTransactionRollbackOnFailure() {
        taskRepository.deleteAll();

        TaskDTO taskDTO = TaskDTO.builder()
                .title("Task to Fail")
                .description("This task will trigger a failure")
                .priority(3)
                .status("TODO")
                .dueDate(LocalDate.now().plusDays(5))
                .assignedUserId(assignedUser1.getId().toString())
                .reporterUserId(reporterUser1.getId().toString())
                .shouldNotifyUser(true)
                .build();

        Mockito.doThrow(new RuntimeException("Unexpected Exception While Transactional")).when(
                taskMapper
        ).toDTO(Mockito.any());

        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());

        assertThrows(RuntimeException.class, () -> taskService.createTask(taskDTO));


        // Verify that no data was persisted due to rollback
        assertEquals(0, taskRepository.count());
        assertEquals(0, historyRepository.count());
        assertEquals(0, notificationRepository.count());
    }


    @Test
    public void testDeleteTask() {
        TaskDTO result = taskService.deleteTask(existingTask.getId());
        assertNotNull(result);
        assertEquals("Existing Task", result.getTitle());
        assertFalse(taskRepository.findById(existingTask.getId()).isPresent());
    }

    @Test
    public void testGetTaskById() {
        TaskDTO result = taskService.getTaskById(existingTask.getId());
        assertNotNull(result);
        assertEquals("Existing Task", result.getTitle());
    }

    @Test
    public void testGetTaskById_NotFound() {
        assertThrows(NotFoundException.class, () -> taskService.getTaskById(UUID.randomUUID()));
    }

    @Test
    public void testGetAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        assertEquals(1, tasks.size());
        assertEquals("Existing Task", tasks.get(0).getTitle());
    }

    @Test
    public void testGetTasksOrderedByPriorityAndDueDate() {
        List<TaskDTO> tasks = taskService.getTasksOrderedByPriorityAndDueDate();
        assertEquals(1, tasks.size());
        assertEquals("Existing Task", tasks.get(0).getTitle());
    }

    @Test
    public void testAssignTask() {
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());
        TaskDTO result = taskService.assignTask(existingTask.getId(), assignedUser2.getId().toString());
        assertNotNull(result);
        assertEquals(assignedUser2.getId().toString(), result.getAssignedUserId());
    }

    @ParameterizedTest
    @MethodSource("provideTaskChangesForFindDifferences")
    public void testFindDifferences(TaskDTO newTaskDTO, String expectedDifference) {
        Task existingTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Existing Task")
                .description("Existing Description")
                .priority(1)
                .status(Status.TODO)
                .dueDate(LocalDate.now())
                .assignedUser(assignedUser1)
                .reporterUser(assignedUser1)
                .build();

        String differences = taskService.findDifferences(existingTask, newTaskDTO);
        assertTrue(differences.contains(expectedDifference));
    }

    private static Stream<Arguments> provideTaskChangesForFindDifferences() {
        User newAssignedUser = User.builder().id(UUID.randomUUID()).build();
        User newReporterUser = User.builder().id(UUID.randomUUID()).build();

        return Stream.of(
                Arguments.of(
                        TaskDTO.builder()
                                .title("Updated Task")
                                .description("Existing Description")
                                .priority(1)
                                .status("TODO")
                                .dueDate(LocalDate.now())
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Title got change from [Existing Task] to [Updated Task]"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Updated Description")
                                .priority(1)
                                .status("TODO")
                                .dueDate(LocalDate.now())
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Description got change from [Existing Description] to [Updated Description]"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Existing Description")
                                .priority(3)
                                .status("TODO")
                                .dueDate(LocalDate.now())
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Priority got change from [1] to [3]"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Existing Description")
                                .priority(1)
                                .status("IN_PROGRESS")
                                .dueDate(LocalDate.now())
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Status got change from [TODO] to [IN_PROGRESS]"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Existing Description")
                                .priority(1)
                                .status("TODO")
                                .dueDate(LocalDate.now().plusDays(5))
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Due Date got change from"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Existing Description")
                                .priority(1)
                                .status("TODO")
                                .dueDate(LocalDate.now())
                                .assignedUserId(UUID.randomUUID().toString())
                                .reporterUserId(newReporterUser.getId().toString())
                                .build(),
                        "Assigned User ID got change from"
                ),
                Arguments.of(
                        TaskDTO.builder()
                                .title("Existing Task")
                                .description("Existing Description")
                                .priority(1)
                                .status("TODO")
                                .dueDate(LocalDate.now())
                                .assignedUserId(newAssignedUser.getId().toString())
                                .reporterUserId(UUID.randomUUID().toString())
                                .build(),
                        "Reporter User ID got change from"
                )
        );
    }
}
