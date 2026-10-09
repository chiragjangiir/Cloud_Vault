package com.cloudvault.config;

import com.cloudvault.security.CloudVaultAuthenticationProvider;
import com.cloudvault.security.CloudVaultUserDetailsService;
import com.cloudvault.security.SessionTrackingListener;
import com.cloudvault.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Security rules: every protected resource requires authentication AND
 * authorization. Login is rate-limited server-side, sessions are tracked,
 * CSRF protects all state-changing requests.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SessionTrackingListener sessionTracking;
    private final RateLimitService rateLimitService;
    private final com.cloudvault.security.SessionEnforcementFilter sessionEnforcementFilter;
    private final int loginRateLimit;

    public SecurityConfig(SessionTrackingListener sessionTracking,
                          RateLimitService rateLimitService,
                          com.cloudvault.security.SessionEnforcementFilter sessionEnforcementFilter,
                          @Value("${app.rate-limit.login-per-10min:10}") int loginRateLimit) {
        this.sessionTracking = sessionTracking;
        this.rateLimitService = rateLimitService;
        this.sessionEnforcementFilter = sessionEnforcementFilter;
        this.loginRateLimit = loginRateLimit;
    }

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${app.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(CloudVaultUserDetailsService userDetailsService,
                                                        PasswordEncoder passwordEncoder,
                                                        com.cloudvault.repository.UserRepository users) {
        return new CloudVaultAuthenticationProvider(userDetailsService, passwordEncoder, users);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider provider,
                                           SessionRegistry sessionRegistry) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password", "/share/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/password/reset-request",
                        "/api/v1/auth/password/reset").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureUrl("/login?error")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll())
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(10)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?expired"))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Raw pass-through handler: the XSRF-TOKEN cookie value is the
                // accepted header value, so JS clients (app.js) and API clients
                // can echo the cookie directly. The default Xor handler would
                // accept only view-encoded tokens and break the cookie flow.
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler()))            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            // 'unsafe-inline' for STYLE only: the server renders dynamic
                            // gauge/progress widths as style attributes (th:style), and
                            // per-value hashes are impossible for dynamic numbers.
                            // Scripts remain 'self'-only; frame-ancestors denied.
                            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'"))
                .frameOptions(f -> f.deny()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/problem+json");
                        response.getWriter().write("{\"title\":\"UNAUTHORIZED\",\"status\":401,\"detail\":\"Authentication required\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/problem+json");
                        response.getWriter().write("{\"title\":\"FORBIDDEN\",\"status\":403,\"detail\":\"Access denied\"}");
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    }
                }))
            .authenticationProvider(provider)
            .addFilterBefore(loginRateLimitFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(sessionEnforcementFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            sessionTracking.sessionAuthenticated(request.getSession(true));
            response.sendRedirect(request.getContextPath() + "/app");
        };
    }

    private AuthenticationFailureHandler loginFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error");
    }

    /**
     * Real fixed-window rate limit on login attempts, keyed by client IP and
     * submitted username. Returns 429 when exhausted.
     */
    private OncePerRequestFilter loginRateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                boolean isLoginPost = "POST".equals(request.getMethod())
                        && "/login".equals(request.getRequestURI());
                if (isLoginPost) {
                    String ip = request.getRemoteAddr();
                    String user = request.getParameter("username");
                    String key = ip + "|" + (user == null ? "" : user.toLowerCase());
                    try {
                        rateLimitService.consume("login", key, loginRateLimit, Duration.ofMinutes(10));
                    } catch (com.cloudvault.web.error.ApiException e) {
                        response.setStatus(429);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many login attempts; try again later\"}");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
