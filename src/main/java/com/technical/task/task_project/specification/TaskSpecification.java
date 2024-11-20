package com.technical.task.task_project.specification;

import com.technical.task.task_project.model.Task;
import com.technical.task.task_project.model.enums.Status;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskSpecification {

    public static Specification<Task> searchBySpecification(
            String title,
            String description,
            Status status,
            Integer minPriority,
            Integer maxPriority,
            UUID assignedUserId,
            UUID reporterUserId,
            LocalDate startDate,
            LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            if (description != null && !description.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPriority != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priority"), minPriority));
            }
            if (maxPriority != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("priority"), maxPriority));
            }

            if (assignedUserId != null && !assignedUserId.toString().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("assignedUser").get("id"), assignedUserId));
            }

            if (reporterUserId != null && !reporterUserId.toString().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("reporterUser").get("id"), reporterUserId));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
