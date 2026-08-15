package com.community.signal.review.config;

import com.community.signal.review.security.JwtAuthenticationFilter;
import com.community.signal.review.service.GitHubOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GitHubOAuthService gitHubOAuthService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // ── Nível 1: Público (somente leitura) ─────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/drafts", "/api/drafts/*", "/api/drafts/stats").permitAll()

                // ── Nível 2: Autenticado (ações de review) ─────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/drafts/*/approve",
                    "/api/drafts/*/reject",
                    "/api/drafts/*/review").authenticated()

                // ── Nível 3: Admin ─────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")

                // ── Auth + OAuth + health ──────────────────────────────────
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization"))
                .successHandler((request, response, authentication) -> {
                    try {
                        String jwt = gitHubOAuthService.handleOAuthSuccess(authentication);
                        log.info("github.oauth.handler.success redirecting_to_frontend");
                        response.setStatus(HttpServletResponse.SC_FOUND);
                        response.setHeader("Location", frontendUrl + "/oauth2/callback#token=" + jwt);
                    } catch (SecurityException e) {
                        log.warn("github.oauth.handler.denied reason={}", e.getMessage());
                        response.sendRedirect(frontendUrl + "/login?error=unauthorized");
                    } catch (Exception e) {
                        log.error("github.oauth.handler.error", e);
                        response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    log.error("github.oauth.failure", exception);
                    response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            frontendUrl,
            "https://community-signal-black.vercel.app",
            "http://localhost:4200"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
