package ru.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest request) {
        return response(e.getStatus(), e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Проверьте введённые данные", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Некорректный запрос", request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> unauthorized(AuthenticationException e, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Требуется вход или токен недействителен", request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException e, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Недостаточно прав", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Запись уже существует или используется другими данными", request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> missing(Exception e, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Ресурс не найден", request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> method(Exception e, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "Метод не поддерживается", request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMedia(Exception e, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Используйте Content-Type: application/json", request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> unacceptableMedia(Exception e, HttpServletRequest request) {
        return response(HttpStatus.NOT_ACCEPTABLE, "Ответ доступен в формате application/json", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Request failed: {}", request.getRequestURI(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, Map.of());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message,
            HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ErrorResponse(Instant.now(), status.value(),
            message, request.getRequestURI(), errors));
    }
}
