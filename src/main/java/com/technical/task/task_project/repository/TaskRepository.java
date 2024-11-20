package com.technical.task.task_project.repository;

import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    List<Task> findAllByStatusNotAndDueDateAfterOrderByPriorityDescDueDateAsc(Status status, LocalDate currentDate);

}
