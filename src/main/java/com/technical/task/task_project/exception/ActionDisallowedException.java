package com.technical.task.task_project.exception;

public class ActionDisallowedException extends RuntimeException {
    public ActionDisallowedException(String message) {
        super(message);
    }
}
