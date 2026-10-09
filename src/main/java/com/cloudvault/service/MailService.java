package com.cloudvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends real email through the configured SMTP server. When no mail host is
 * configured the service honestly reports unavailable — no fake delivery.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String from;
    private final String host;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${app.mail.from:no-reply@cloudvault.local}") String from) {
        this.senderProvider = senderProvider;
        this.host = host;
        this.from = from;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank();
    }

    /**
     * Sends the password reset link. The raw token only ever exists in the
     * email body — it is never logged.
     */
    public void sendPasswordReset(String toEmail, String rawToken) {
        if (!isConfigured()) {
            throw new IllegalStateException("SMTP is not configured");
        }
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP is not configured");
        }
        String link = baseURL() + "/reset-password?token=" + rawToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Cloud Vault password reset");
        msg.setText("A password reset was requested for your Cloud Vault account.\n\n"
                + "Reset link (valid for 30 minutes):\n" + link + "\n\n"
                + "If you did not request this, you can ignore this email.");
        sender.send(msg);
        log.info("Password reset email queued for {}", toEmail);
    }

    private String baseURL() {
        return System.getenv().getOrDefault("APP_PUBLIC_URL", "http://localhost:8080");
    }
}
