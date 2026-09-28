package com.sidis.authservice.Users.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name="company_colaborators")
@Inheritance(strategy = InheritanceType.JOINED)
public class Collaborator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private UUID collaboratorId = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_colaborators_roles", joinColumns = @JoinColumn(name = "colaborator_pk"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    public Collaborator() {
    }

    public Collaborator(String username, String email, String password, Set<Role> roles) {
        Assert.hasText(username, "name must not be blank");
        Assert.hasText(email, "email must not be blank");
        Assert.hasText(password, "password must not be blank");
        Assert.notNull(roles, "roles must not be null");
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public Long getPk() { return pk; }
    public UUID getCollaboratorId() { return collaboratorId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Set<Role> getRoles() { return roles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Collaborator c = (Collaborator) o;
        return Objects.equals(collaboratorId, c.collaboratorId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(collaboratorId);
    }

}
