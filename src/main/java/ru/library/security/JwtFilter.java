package ru.library.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.library.repository.UserRepository;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final HandlerExceptionResolver resolver;

    public JwtFilter(JwtService jwt, UserRepository users,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwt = jwt;
        this.users = users;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwt.subject(header.substring(7));
                var user = users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Unknown user"));
                var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException | BadCredentialsException e) {
                SecurityContextHolder.clearContext();
                resolver.resolveException(request, response, null, new BadCredentialsException("Invalid token", e));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
