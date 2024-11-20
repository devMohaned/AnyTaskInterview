package com.technical.task.task_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.model.enums.Status;
import com.technical.task.task_project.repository.HistoryRepository;
import com.technical.task.task_project.repository.NotificationRepository;
import com.technical.task.task_project.repository.TaskRepository;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.security.jwt.JwtUtil;
import com.technical.task.task_project.service.EmailService;
import com.technical.task.task_project.service.UserService;
import com.technical.task.task_project.service.dto.TaskDTO;
import com.technical.task.task_project.service.mapper.TaskMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("junit")
public class TaskControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @SpyBean
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private User adminUser;
    private User scrumUser;
    private User developerUser;


    private final TaskMapperImpl taskMapper = new TaskMapperImpl();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        Mockito.doNothing().when(emailService).sendEmail(Mockito.any(), Mockito.any(), Mockito.any());

        notificationRepository.deleteAll();
        historyRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = createUser("admin@example.com", "Admin User", Set.of(Role.ADMIN));
        scrumUser = createUser("scrum@example.com", "Scrum User", Set.of(Role.SCRUM));
        developerUser = createUser("developer@example.com", "Developer User", Set.of(Role.DEVELOPER));

        adminToken = jwtUtil.generateToken(adminUser.getEmail(), Set.of("ROLE_ADMIN", "ROLE_SCRUM"));
    }

    private User createUser(String email, String name, Set<Role> roles) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("password") // Use real encryption in production
                .name(name)
                .roles(roles)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    public void testCreateTask_Success() throws Exception {

        initAdminContext();
        TaskDTO taskDTO = TaskDTO.builder()
                .title("Task 1")
                .description("Test Task 1")
                .status(Status.TODO.name())
                .priority(1)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUserId(developerUser.getId().toString())
                .reporterUserId(adminUser.getId().toString())
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andDo(print())
                .andExpect(status().isOk());

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size(), "Task count should be 1");
        Task savedTask = tasks.get(0);
        assertEquals("Task 1", savedTask.getTitle());
        assertEquals(Status.TODO, savedTask.getStatus());
        assertEquals(developerUser.getId(), savedTask.getAssignedUser().getId());
        assertEquals(adminUser.getId(), savedTask.getReporterUser().getId());
    }

    @Test
    public void testUpdateTask_Success() throws Exception {
        initAdminContext();
        TaskDTO taskDTO = createTestTask("Task 2", adminUser, developerUser);

        taskDTO.setTitle("Updated Task");
        taskDTO.setDescription("Updated Description");
        taskDTO.setPriority(2);
        Task savedTask = taskRepository.save(taskMapper.toEntity(taskDTO, userRepository.findById(adminUser.getId()).get(), userRepository.findById(developerUser.getId()).get()));


        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andDo(print())
                .andExpect(status().isOk());

        Task updatedTask = taskRepository.findAll().get(0);
        assertNotNull(updatedTask, "Updated task should exist");
        assertEquals("Updated Task", updatedTask.getTitle());
        assertEquals(2, updatedTask.getPriority());
    }

    @Test
    public void testGetAllTasks() throws Exception {
        initAdminContext();
        TaskDTO task1 = createTestTask("Task 3", adminUser, developerUser);
        TaskDTO task2 = createTestTask("Task 4", scrumUser, developerUser);
        Task savedTask1 = taskRepository.save(taskMapper.toEntity(task1, userRepository.findById(adminUser.getId()).get(), userRepository.findById(developerUser.getId()).get()));
        Task savedTask2 = taskRepository.save(taskMapper.toEntity(task2, userRepository.findById(scrumUser.getId()).get(), userRepository.findById(developerUser.getId()).get()));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.[0].title").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].title").isNotEmpty());
    }

    @Test
    public void testAssignTask_Success() throws Exception {
        initAdminContext();
        TaskDTO taskDTO = createTestTask("Task 5", adminUser, scrumUser);
        Task savedTask = taskRepository.save(taskMapper.toEntity(taskDTO, userRepository.findById(adminUser.getId()).get(), userRepository.findById(scrumUser.getId()).get()));

        mockMvc.perform(put("/api/tasks/" + savedTask.getId() + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("assignedUserId", developerUser.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteExistingTask_Success() throws Exception {
        initAdminContext();
        TaskDTO taskDTO = createTestTask("Test Task", adminUser, adminUser);
        Task savedTask = taskRepository.save(taskMapper.toEntity(taskDTO, userRepository.findById(adminUser.getId()).get(), userRepository.findById(scrumUser.getId()).get()));

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        assertTrue(taskRepository.findById(savedTask.getId()).isEmpty(), "Task should be deleted");
    }

    @Test
    public void testDeleteNonExistingTask_NotFound() throws Exception {
        initAdminContext();
        UUID nonExistingTaskId = UUID.randomUUID();
        mockMvc.perform(delete("/api/tasks/" + nonExistingTaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetTaskById_Success() throws Exception {
        initAdminContext();
        TaskDTO taskDTO = createTestTask("Test Task", adminUser, developerUser);
        Task savedTask = taskRepository.save(taskMapper.toEntity(taskDTO, userRepository.findById(adminUser.getId()).get(), userRepository.findById(scrumUser.getId()).get()));

        mockMvc.perform(get("/api/tasks/" + savedTask.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Task retrievedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(retrievedTask, "Task should exist");
        assertEquals(taskDTO.getTitle(), retrievedTask.getTitle());
    }

    @Test
    public void testGetTaskById_NotFound() throws Exception {
        initAdminContext();
        mockMvc.perform(get("/api/tasks/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSearchAndFilterTasks() throws Exception {
        initAdminContext();
        TaskDTO testTask1 = createTestTask("Task 1", adminUser, developerUser);
        TaskDTO testTask2 = createTestTask("Task 2", adminUser, developerUser);

        Task savedTask1 = taskRepository.save(taskMapper.toEntity(testTask1, userRepository.findById(adminUser.getId()).get(), userRepository.findById(scrumUser.getId()).get()));
        Task savedTask2 = taskRepository.save(taskMapper.toEntity(testTask2, userRepository.findById(adminUser.getId()).get(), userRepository.findById(scrumUser.getId()).get()));


        mockMvc.perform(get("/api/tasks/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("title", "Task")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // Assert that tasks match the search criteria
        assertEquals(2, taskRepository.findAll().size(), "Two tasks should exist in the repository");
    }

    @Test
    public void testSearchAndFilterTasks_NoResults() throws Exception {
        initAdminContext();
        mockMvc.perform(get("/api/tasks/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("title", "Nonexistent Task")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        assertEquals(0, taskRepository.findAll().stream()
                .filter(task -> task.getTitle().contains("Nonexistent Task"))
                .count(), "No tasks should match the criteria");
    }

    private TaskDTO createTestTask(String title, User reporter, User assignee) throws Exception {
        TaskDTO taskDTO = TaskDTO.builder()
                .title(title)
                .description("Description of " + title)
                .status(Status.TODO.name())
                .priority(1)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUserId(assignee.getId().toString())
                .reporterUserId(reporter.getId().toString())
                .build();


        return taskDTO;
    }

    private static void initAdminContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SCRUM")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}