package com.cloudvault.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Thymeleaf 3.1 removed the #request expression object from templates, so the
 * request URI used for nav highlighting is exposed as a model attribute here
 * instead — available to every controller-rendered page.
 */
@ControllerAdvice
public class UiAdvice {

    /**
     * Format adapter exposed to every template as {@code fmt}.
     *
     * <p>Thymeleaf evaluates {@code th:attr} and fallback attributes (e.g.
     * {@code th:style}) in a RESTRICTED context that forbids SpEL static
     * references like {@code T(...)} — instance access on a model object is
     * allowed, so templates use {@code ${fmt.percent(a,b)}} there instead of
     * {@code ${T(com.cloudvault.service.Format).percent(a,b)}}, which is only
     * legal in normal-context attributes such as {@code th:text}.</p>
     */
    public static final class FormatAdapter {
        public String bytes(long value) { return com.cloudvault.service.Format.bytes(value); }
        public String dateTime(java.time.Instant instant) { return com.cloudvault.service.Format.dateTime(instant); }
        public String date(java.time.Instant instant) { return com.cloudvault.service.Format.date(instant); }
        public String percent(long used, long total) { return com.cloudvault.service.Format.percent(used, total); }
        public double percentNumber(long used, long total) { return com.cloudvault.service.Format.percentNumber(used, total); }
    }

    private static final FormatAdapter FMT = new FormatAdapter();

    @ModelAttribute
    public void commonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("fmt", FMT);
    }
}
