package com.example.emp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.extractUsername(token);
                String role     = jwtUtil.extractRole(token);

                if (role == null || role.isBlank()) {
                    log.warn("[JWT] role 클레임이 없음 - username: {}", username);
                    chain.doFilter(request, response);
                    return;
                }

                // ROLE_ADMIN 또는 ROLE_USER 권한 부여
                String authority = "ROLE_" + role.toUpperCase().trim();
                log.debug("[JWT] 인증 설정 - username: {}, authority: {}", username, authority);
                var authorities = List.of(new SimpleGrantedAuthority(authority));
                var auth = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.debug("[JWT] 유효하지 않은 토큰 - URI: {}", request.getRequestURI());
            }
        }

        chain.doFilter(request, response);
    }
}
