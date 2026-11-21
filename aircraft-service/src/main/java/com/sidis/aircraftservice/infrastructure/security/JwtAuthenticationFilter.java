package com.sidis.aircraftservice.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private Environment env;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            
                // 1. Extrai o claim de forma segura
                Object rolesClaim = claims.get("roles");
                List<String> roles = new ArrayList<>();
                
                // 2. Adapta-se automaticamente se for Lista ou String
                if (rolesClaim instanceof List) {
                    roles = (List<String>) rolesClaim;
                } else if (rolesClaim instanceof String) {
                    roles.add((String) rolesClaim);
                }
            
                // 3. Mapeia para Spring Security Authorities
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                        .toList();
            
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            
                SecurityContextHolder.getContext().setAuthentication(authentication);
            
            } catch (Exception e) {
                // FIM DO SILÊNCIO: Imprime o erro exato na consola para sabermos quem é o culpado
                System.err.println("ERRO AO PROCESSAR JWT: " + e.getMessage());
                e.printStackTrace();
                SecurityContextHolder.clearContext();
            }

        }

        filterChain.doFilter(request, response);
    }
}