package com.community.signal.review.security;

import com.community.signal.review.domain.ReviewUser;
import com.community.signal.review.repository.ReviewUserRepository;
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
    private final ReviewUserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 1. Validate signature + expiration
        if (!tokenProvider.validateToken(token)) {
            log.warn("jwt.filter.invalid.token uri={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String username = tokenProvider.getUsernameFromToken(token);
        List<String> tokenRoles = tokenProvider.getRolesFromToken(token);

        // 2. Validate user exists and is active in database
        ReviewUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("jwt.filter.user.not.found username={} uri={}", username, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (!user.isActive()) {
            log.warn("jwt.filter.user.inactive username={} uri={}", username, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Use DB role (authoritative) instead of token role (defensive: prevents role escalation)
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("jwt.filter.authenticated username={} role={} uri={}", username, user.getRole(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }
}
