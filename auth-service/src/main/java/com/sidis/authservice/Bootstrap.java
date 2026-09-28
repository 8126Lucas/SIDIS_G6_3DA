package com.sidis.authservice;

import com.sidis.authservice.Users.domain.Collaborator;
import com.sidis.authservice.Users.domain.Role;
import com.sidis.authservice.Users.infrastructure.CollaboratorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
//@Profile("dev")
public class Bootstrap implements ApplicationRunner {

    private final CollaboratorRepository collaboratorRepository;
    private final PasswordEncoder passwordEncoder;


    public Bootstrap(CollaboratorRepository collaboratorRepository, PasswordEncoder passwordEncoder) {
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (collaboratorRepository.count() == 0) {
            seedCollaborators();
        }
    }

    private void seedCollaborators() {
        collaboratorRepository.saveAll(List.of(
                new Collaborator("admin", "admin@email.com",
                        passwordEncoder.encode("admin123"), Set.of(Role.ADMIN)),

                new Collaborator("atcc", "atcc@email.com",
                        passwordEncoder.encode("atcc123"), Set.of(Role.ATCC)),

                new Collaborator("backoffice", "backoffice@email.com",
                        passwordEncoder.encode("backoffice123"), Set.of(Role.BACKOFFICE)),

                new Collaborator("technician", "technician@email.com",
                        passwordEncoder.encode("technician123"), Set.of(Role.MAINTENANCE_TECHNICIAN)),

                new Collaborator("supervisor", "supervisor@email.com",
                        passwordEncoder.encode("supervisor123"), Set.of(Role.MAINTENANCE_SUPERVISOR))
        ));
    }
}