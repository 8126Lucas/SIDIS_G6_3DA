package com.sidis.authservice.Users.infrastructure;

import com.sidis.authservice.Users.domain.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {
    Optional<Collaborator> findByEmail(String email);
    Optional<Collaborator> findByUsername(String username);
}
