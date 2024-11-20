package com.technical.task.task_project.exception;

import com.technical.task.task_project.exception.model.ErrorResponse;
import io.micrometer.tracing.Tracer;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        String traceId = getTraceIdFromRequest();

        ErrorResponse response = new ErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.name(),
                HttpStatus.BAD_REQUEST.value(),
                traceId,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = getTraceIdFromRequest();

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.name(),
                HttpStatus.BAD_REQUEST.value(),
                traceId,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(NotFoundException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.name(),
                HttpStatus.NOT_FOUND.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {AlreadyExistsException.class, DuplicateException.class})
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(RuntimeException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT.name(),
                HttpStatus.CONFLICT.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                "Invalid username or password.",
                HttpStatus.UNAUTHORIZED.name(),
                HttpStatus.UNAUTHORIZED.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                "Authentication failed.",
                HttpStatus.UNAUTHORIZED.name(),
                HttpStatus.UNAUTHORIZED.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                "Access denied.",
                HttpStatus.FORBIDDEN.name(),
                HttpStatus.FORBIDDEN.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ActionDisallowedException.class)
    public ResponseEntity<ErrorResponse> handleActionDisallowedException(ActionDisallowedException ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.FORBIDDEN.name(),
                HttpStatus.FORBIDDEN.value(),
                traceId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        String traceId = getTraceIdFromRequest();
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                traceId,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getTraceIdFromRequest() {
        return tracer.currentSpan() != null
                ? Objects.requireNonNull(tracer.currentSpan()).context().traceId()
                : "No Trace Context Available";
    }
}
