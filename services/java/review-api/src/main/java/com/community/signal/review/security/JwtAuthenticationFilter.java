package com.community.signal.review.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract Authorization header from request
        String authHeader = request.getHeader("Authorization");

        // 2. If header is null or does not start with "Bearer " → continue chain and return
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token
        String token = authHeader.substring(7);

        // 4. Validate token and set SecurityContext
        if (tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            List<String> roles = tokenProvider.getRolesFromToken(token);

            // c. Build List<GrantedAuthority> from roles
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // d. Create UsernamePasswordAuthenticationToken
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            // e. Set details
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // f. Set SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("jwt.filter.authenticated username={} uri={}", username, request.getRequestURI());
        } else {
            // 6. Log WARN if token is present but invalid
            log.warn("jwt.filter.invalid.token uri={}", request.getRequestURI());
        }

        // 5. Call filterChain.doFilter
        filterChain.doFilter(request, response);
    }
}
