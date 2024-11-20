package com.technical.task.task_project.service.mapper;

import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.service.dto.TaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java( com.technical.task.task_project.model.enums.Status.valueOf(taskDTO.getStatus()) )")
    @Mapping(target = "assignedUser", source = "assignedUser")
    @Mapping(target = "reporterUser", source = "reporterUser")
    Task toEntity(TaskDTO taskDTO, User assignedUser, User reporterUser);

    @Mapping(target = "status", expression = "java(task.getStatus().name())")
    @Mapping(target = "assignedUserId", expression = "java(task.getAssignedUser() != null ? task.getAssignedUser().getId().toString() : null)")
    @Mapping(target = "reporterUserId", expression = "java(task.getReporterUser() != null ? task.getReporterUser().getId().toString() : null)")
    TaskDTO toDTO(Task task);
}
