package com.sidis.authservice.Authentication;

import com.sidis.authservice.Users.domain.Role;
import com.sidis.authservice.Authentication.infrastructure.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.*;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(POST, "/auth/login").permitAll();
                    auth.requestMatchers(GET, "/api/welcome").permitAll();
                    auth.requestMatchers("/api/swagger-ui/**").permitAll();
                    auth.requestMatchers("/api/docs/**").permitAll();

                    auth.requestMatchers(POST, "/api/catalog/model").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(PATCH, "/api/catalog/model/{modelName}").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/catalog/rankings").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());

                    auth.requestMatchers(POST, "/api/hangar/aircraft").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/aircraft").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/aircraft/{id}").hasAnyRole(Role.BACKOFFICE.name(),Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(PATCH,"/api/hangar/aircraft/{id}").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/aircraft/{registrationNumber}/compatible-routes").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/aircraft/{registrationNumber}/status").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());

                    auth.requestMatchers(GET,"/api/hangar/analytics/aircraft-availability").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/analytics/operational-hours").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/analytics/status-summary").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/analytics/fuel-efficiency").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/analytics/fuel-efficiency/aircraft").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET,"/api/hangar/analytics/fuel-efficiency/routes").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());

                    auth.requestMatchers(GET,"/api/hangar/view/utilization").permitAll();

                    auth.requestMatchers(POST, "/api/maintenance/record").hasAnyRole(Role.MAINTENANCE_TECHNICIAN.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/maintenance/record/{id}").hasAnyRole(Role.MAINTENANCE_TECHNICIAN.name(),Role.ADMIN.name());
                    auth.requestMatchers(PATCH,"/api/maintenance/{recordId}/complete").hasAnyRole(Role.MAINTENANCE_TECHNICIAN.name(),Role.MAINTENANCE_SUPERVISOR.name(),Role.ADMIN.name());
                    auth.requestMatchers(PATCH, "/api/maintenance/record/{id}").hasAnyRole(Role.MAINTENANCE_TECHNICIAN.name(),Role.ADMIN.name());

                    auth.requestMatchers(POST, "/api/airports").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(POST, "/api/airports/{iataCode}/certifications").hasAnyRole(Role.BACKOFFICE.name(),Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/airports").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/airports/{iataCode}").hasAnyRole(Role.BACKOFFICE.name(),Role.ATCC.name(),Role.ADMIN.name());
                    auth.requestMatchers(PATCH, "/api/airports/{iataCode}/status").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(POST, "/api/airports/import").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());

                    auth.requestMatchers(GET, "/api/routes/export/geojson").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/routes/export/kml").hasAnyRole(Role.BACKOFFICE.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/flights/reports/route-utilization").hasAnyRole(Role.ATCC.name(),Role.ADMIN.name());

                    auth.requestMatchers(GET, "/api/maintenance/parts/inventory").hasAnyRole(Role.MAINTENANCE_SUPERVISOR.name(),Role.ADMIN.name());
                    auth.requestMatchers(GET, "/api/maintenance/parts/low-stock").hasAnyRole(Role.MAINTENANCE_SUPERVISOR.name(),Role.ADMIN.name());

                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                   auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (h2ConsoleEnabled) {
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        } else {
            http.headers(h -> h.frameOptions(f -> f.deny()));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
