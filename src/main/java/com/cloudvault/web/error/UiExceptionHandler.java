package com.cloudvault.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * View-rendering error handling for the server-rendered UI. Renders real error
 * pages with proper HTTP statuses instead of swallowing failures.
 */
@ControllerAdvice(basePackages = "com.cloudvault.web.ui")
public class UiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UiExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView accessDenied(AccessDeniedException e) {
        return view(HttpStatus.FORBIDDEN, "error/error", "Access denied");
    }

    private ModelAndView view(HttpStatus status, String template, String message) {
        ModelAndView mav = new ModelAndView(template);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", message);
        return mav;
    }

    @ExceptionHandler(ApiException.class)
    public ModelAndView handleApi(ApiException e, HttpServletRequest request) {
        if (e.getStatus().is5xxServerError()) {
            log.error("UI failure code={} path={}: {}", e.getCode(), request.getRequestURI(), e.getMessage());
        }
        HttpStatus status = e.getStatus();
        ModelAndView mav = new ModelAndView("error/error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("code", e.getCode());
        mav.addObject("error", e.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled UI error on {} {}", request.getMethod(), request.getRequestURI(), e);
        ModelAndView mav = new ModelAndView("error/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("status", 500);
        mav.addObject("code", "INTERNAL_ERROR");
        mav.addObject("error", "An internal error occurred");
        return mav;
    }
}
