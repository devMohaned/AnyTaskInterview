package com.technical.task.task_project.service;

import com.technical.task.task_project.exception.DuplicateException;
import com.technical.task.task_project.exception.NotFoundException;
import com.technical.task.task_project.model.History;
import com.technical.task.task_project.model.Notification;
import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Status;
import com.technical.task.task_project.repository.HistoryRepository;
import com.technical.task.task_project.repository.NotificationRepository;
import com.technical.task.task_project.repository.TaskRepository;
import com.technical.task.task_project.service.dto.TaskDTO;
import com.technical.task.task_project.service.mapper.TaskMapper;
import com.technical.task.task_project.specification.TaskSpecification;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Log4j2
public class TaskService {

    private final UserService userService;
    private final EmailService emailService;
    private final TaskRepository taskRepository;
    private final HistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        User currentUser = userService.getCurrentUser();

        User assignedUser = userService.validateUserExistence(UUID.fromString(taskDTO.getAssignedUserId()));
        User reportedUser = userService.validateUserExistence(UUID.fromString(taskDTO.getReporterUserId()));
        Task task = taskMapper.toEntity(taskDTO, assignedUser, reportedUser);
        saveTask(task);
        saveHistoryAndNotify(taskDTO.isShouldNotifyUser(), "Task is created", String.format("Task [%s] was assigned to you by [%s]", task.getTitle(), reportedUser.getName()),
                "A task was assigned to you", currentUser, assignedUser, task);
        return taskMapper.toDTO(task);
    }

    private void saveTask(Task task) {
        try {
            taskRepository.save(task);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateException("A task with the same values already exists.");
        }
    }

    private void saveHistoryAndNotify(boolean shouldNotifyUser, String changeDescription, String message, String subject, User currentUser, User assignedUser, Task task) {
        History taskIsCreated = History.builder().changeDate(LocalDateTime.now()).changeDescription(changeDescription).changedBy(currentUser).task(task).build();
        historyRepository.save(taskIsCreated);

        if (shouldNotifyUser) {
            Notification assignedUserNotification = Notification.builder().notificationDate(LocalDateTime.now())
                    .relatedTask(task)
                    .message(message)
                    .user(assignedUser)
                    .build();

            emailService.sendEmail(assignedUser.getEmail(), subject, message);
            notificationRepository.save(assignedUserNotification);
        }
    }

    @Transactional
    public TaskDTO updateTask(UUID taskId, TaskDTO taskDTO) {
        Task existingTask = validateTaskExistence(taskId);

        String foundDifferences = findDifferences(existingTask, taskDTO);
        log.info("Found Those differences for task with UUID [{}]. Changes: [{}]", taskId, foundDifferences);

        existingTask.setTitle(taskDTO.getTitle());
        existingTask.setDescription(taskDTO.getDescription());
        existingTask.setStatus(Status.valueOf(taskDTO.getStatus()));
        existingTask.setPriority(taskDTO.getPriority());
        existingTask.setDueDate(taskDTO.getDueDate());

        User providedAssignedUserId = userService.validateUserExistence(UUID.fromString(taskDTO.getAssignedUserId()));
        existingTask.setAssignedUser(providedAssignedUserId);
        User providedReporterUserId = userService.validateUserExistence(UUID.fromString(taskDTO.getReporterUserId()));
        existingTask.setReporterUser(providedReporterUserId);


        String subject = String.format("Task of UUID [%s] got updated", taskId);
        String changeDescription = String.format("Changes were made for Task of UUID [%s]. Changes [%s]", taskId, foundDifferences);
        User currentUser = userService.getCurrentUser();
        saveTask(existingTask);
        saveHistoryAndNotify(taskDTO.isShouldNotifyUser(), foundDifferences, changeDescription, subject, currentUser, providedAssignedUserId, existingTask);
        return taskMapper.toDTO(existingTask);
    }

    public String findDifferences(Task existingTask, TaskDTO newTask) {
        StringBuilder stringBuilder = new StringBuilder();

        if (!existingTask.getTitle().equals(newTask.getTitle())) {
            stringBuilder.append(String.format("Title got change from [%s] to [%s]. \n", existingTask.getTitle(), newTask.getTitle()));
        }

        if (!existingTask.getDescription().equals(newTask.getDescription())) {
            stringBuilder.append(String.format("Description got change from [%s] to [%s]. \n", existingTask.getDescription(), newTask.getDescription()));
        }

        if (existingTask.getPriority() != newTask.getPriority()) {
            stringBuilder.append(String.format("Priority got change from [%s] to [%s]. \n", existingTask.getPriority(), newTask.getPriority()));
        }

        if (!existingTask.getStatus().name().equals(newTask.getStatus())) {
            stringBuilder.append(String.format("Status got change from [%s] to [%s]. \n", existingTask.getStatus(), newTask.getStatus()));
        }

        if (!existingTask.getDueDate().isEqual(newTask.getDueDate())) {
            stringBuilder.append(String.format("Due Date got change from [%s] to [%s]. \n", existingTask.getDueDate(), newTask.getDueDate()));
        }

        if (!existingTask.getAssignedUser().getId().toString().equals(newTask.getAssignedUserId())) {
            stringBuilder.append(String.format("Assigned User ID got change from [%s] to [%s]. \n", existingTask.getAssignedUser().getId(), newTask.getAssignedUserId()));
        }
        if (!existingTask.getReporterUser().getId().toString().equals(newTask.getReporterUserId())) {
            stringBuilder.append(String.format("Reporter User ID got change from [%s] to [%s]. \n", existingTask.getReporterUser().getId(), newTask.getReporterUserId()));
        }

        return stringBuilder.toString();
    }

    public TaskDTO deleteTask(UUID taskId) {
        Task existingTask = validateTaskExistence(taskId);
        taskRepository.delete(existingTask);
        return taskMapper.toDTO(existingTask);
    }

    public TaskDTO getTaskById(UUID taskId) {
        Task task = validateTaskExistence(taskId);
        return taskMapper.toDTO(task);
    }

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public List<TaskDTO> getTasksOrderedByPriorityAndDueDate() {
        return taskRepository.findAllByStatusNotAndDueDateAfterOrderByPriorityDescDueDateAsc(Status.DONE, LocalDate.now())
                .stream()
                .map(taskMapper::toDTO).toList();
    }

    public TaskDTO assignTask(UUID taskId, String assignedUserId) {
        User currentUser = userService.getCurrentUser();
        Task task = validateTaskExistence(taskId);
        User assignedUser = userService.validateUserExistence(UUID.fromString(assignedUserId));
        task.setAssignedUser(assignedUser);
        saveTask(task);
        String message = String.format("Task of UUID [%s] was assigned to you", taskId);
        saveHistoryAndNotify(true, message, message, "New Task is assigned to you", currentUser, assignedUser, task);
        return taskMapper.toDTO(task);
    }


    public Task validateTaskExistence(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> {
            log.warn("Failed to get a task with UUID: [{}]", id);
            throw new NotFoundException("Task does not exist");
        });
    }

    public Page<TaskDTO> searchAndFilterTasks(String title, String description, Status status, Integer minPriority, Integer maxPriority, UUID assignedUserId, UUID reporterUserId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findAll(
                TaskSpecification.searchBySpecification(title, description, status, minPriority, maxPriority, assignedUserId, reporterUserId, startDate, endDate),
                pageable
        );
        return taskPage.map(taskMapper::toDTO);
    }
}