package com.sidis.authservice.Authentication.application;

import com.sidis.authservice.Users.infrastructure.CollaboratorRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CollaboratorDetailsService implements UserDetailsService {

    private final CollaboratorRepository collaboratorRepository;

    public CollaboratorDetailsService(CollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var collaborator = collaboratorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Patron not found"));

        var authorities = collaborator.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                collaborator.getUsername(),
                collaborator.getPassword(),
                authorities
        );
    }
}
