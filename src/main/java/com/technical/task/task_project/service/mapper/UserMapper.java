package com.technical.task.task_project.service.mapper;

import com.technical.task.task_project.model.User;
import com.technical.task.task_project.service.dto.UserDTO;
import com.technical.task.task_project.service.dto.UserRegistrationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "isActive", source = "active")
    UserDTO toDTO(User user);

    @Mapping(target = "password", ignore = true) // Password shouldn't be mapped directly for updates
    @Mapping(target = "isActive", source = "userDTO.active")
    User toEntity(UserDTO userDTO);

    @Mapping(target = "id", ignore = true) // ID is generated and should be ignored
    @Mapping(target = "isActive", constant = "true")
    User toEntity(UserRegistrationDTO userRegistrationDTO);
}
