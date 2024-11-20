package com.technical.task.task_project.service.mapper;

import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Status;
import com.technical.task.task_project.service.dto.TaskDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TaskMapperTest {

    private TaskMapperImpl taskMapper;

    @BeforeEach
    public void setUp() {
        taskMapper = new TaskMapperImpl();
    }

    @ParameterizedTest
    @NullSource
    public void testToEntity_AllInputsNull(TaskDTO taskDTO) {
        Task result = taskMapper.toEntity(taskDTO, null, null);
        assertNull(result, "Expected null when all inputs are null");
    }

    @ParameterizedTest
    @MethodSource("provideTaskDTOs")
    public void testToEntity_WithTaskDTOOnly(TaskDTO taskDTO) {
        Task result = taskMapper.toEntity(taskDTO, null, null);

        assertNotNull(result);
        assertEquals(taskDTO.getTitle(), result.getTitle());
        assertEquals(taskDTO.getDescription(), result.getDescription());
        assertEquals(taskDTO.getPriority(), result.getPriority());
        assertEquals(taskDTO.getDueDate(), result.getDueDate());
        assertEquals(Status.valueOf(taskDTO.getStatus()), result.getStatus());
        assertNull(result.getAssignedUser());
        assertNull(result.getReporterUser());
    }

    private static Stream<Arguments> provideTaskDTOs() {
        return Stream.of(
                Arguments.of(TaskDTO.builder()
                        .title("Task 1")
                        .description("Description 1")
                        .priority(1)
                        .dueDate(LocalDate.now())
                        .status("TODO")
                        .build()),
                Arguments.of(TaskDTO.builder()
                        .title("Task 2")
                        .description("Description 2")
                        .priority(2)
                        .dueDate(LocalDate.now().plusDays(1))
                        .status("IN_PROGRESS")
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("provideUsersAndTaskDTO")
    public void testToEntity_WithAssignedUserAndReporterUser(TaskDTO taskDTO, User assignedUser, User reporterUser) {
        Task result = taskMapper.toEntity(taskDTO, assignedUser, reporterUser);

        assertNotNull(result);
        assertEquals(taskDTO.getTitle(), result.getTitle());
        assertEquals(taskDTO.getDescription(), result.getDescription());
        assertEquals(taskDTO.getPriority(), result.getPriority());
        assertEquals(taskDTO.getDueDate(), result.getDueDate());
        assertEquals(Status.valueOf(taskDTO.getStatus()), result.getStatus());
        assertEquals(assignedUser, result.getAssignedUser());
        assertEquals(reporterUser, result.getReporterUser());
    }

    private static Stream<Arguments> provideUsersAndTaskDTO() {
        TaskDTO taskDTO = TaskDTO.builder()
                .title("Task with Users")
                .description("Description")
                .priority(3)
                .dueDate(LocalDate.now().plusDays(5))
                .status("DONE")
                .build();

        User assignedUser = User.builder().id(UUID.randomUUID()).build();
        User reporterUser = User.builder().id(UUID.randomUUID()).build();

        return Stream.of(
                Arguments.of(taskDTO, assignedUser, reporterUser)
        );
    }

    @ParameterizedTest
    @NullSource
    public void testToDTO_NullTask(Task task) {
        TaskDTO result = taskMapper.toDTO(task);
        assertNull(result, "Expected null when input Task is null");
    }

    @ParameterizedTest
    @MethodSource("provideTasksForToDTO")
    public void testToDTO_WithTask(Task task) {
        TaskDTO result = taskMapper.toDTO(task);

        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        assertEquals(task.getDescription(), result.getDescription());
        assertEquals(task.getPriority(), result.getPriority());
        assertEquals(task.getDueDate(), result.getDueDate());
        assertEquals(task.getStatus().name(), result.getStatus());
        assertEquals(task.getAssignedUser() != null ? task.getAssignedUser().getId().toString() : null, result.getAssignedUserId());
        assertEquals(task.getReporterUser() != null ? task.getReporterUser().getId().toString() : null, result.getReporterUserId());
    }

    private static Stream<Arguments> provideTasksForToDTO() {
        Task taskWithUsers = Task.builder()
                .title("Task with Users")
                .description("Task Description")
                .priority(3)
                .dueDate(LocalDate.now())
                .status(Status.TODO)
                .assignedUser(User.builder().id(UUID.randomUUID()).build())
                .reporterUser(User.builder().id(UUID.randomUUID()).build())
                .build();

        Task taskWithoutUsers = Task.builder()
                .title("Task without Users")
                .description("Another Task")
                .priority(5)
                .dueDate(LocalDate.now().plusDays(3))
                .status(Status.IN_PROGRESS)
                .build();

        return Stream.of(
                Arguments.of(taskWithUsers),
                Arguments.of(taskWithoutUsers)
        );
    }

}
