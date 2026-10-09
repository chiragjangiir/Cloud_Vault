package com.cloudvault.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

/**
 * JSON error handling for /api/v1/**. Returns RFC 7807 ProblemDetail with
 * correct HTTP status codes. Never leaks stack traces or secrets.
 */
@RestControllerAdvice(basePackages = "com.cloudvault.web.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<ProblemDetail> problem(ApiException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        pd.setTitle(e.getCode());
        pd.setType(URI.create("https://cloudvault.local/errors/" + e.getCode().toLowerCase()));
        pd.setProperty("code", e.getCode());
        return ResponseEntity.status(e.getStatus()).body(pd);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(code);
        pd.setType(URI.create("https://cloudvault.local/errors/" + code.toLowerCase()));
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException e, HttpServletRequest request) {
        if (e.getStatus().is5xxServerError()) {
            log.error("API failure code={} path={}: {}", e.getCode(), request.getRequestURI(), e.getMessage());
        }
        return problem(e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    @ExceptionHandler({BadCredentialsException.class, LockedException.class, DisabledException.class})
    public ResponseEntity<ProblemDetail> handleAuth(RuntimeException e) {
        String msg = e instanceof BadCredentialsException ? "Invalid username or password"
                : "Account unavailable; contact your administrator";
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String detail = fe != null ? fe.getField() + " " + fe.getDefaultMessage() : "Validation failed";
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUpload(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Uploaded file exceeds the allowed size");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled API error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal error occurred");
    }
}
