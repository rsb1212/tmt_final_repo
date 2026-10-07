package com.testmgmt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.testmgmt.entity.User;
import com.testmgmt.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    /** Case-insensitive lookup — IDEM/AD may return the email in mixed case
     *  (e.g. "Someshwar.Phadatare@bajajallianz.co.in") while the DB stores it
     *  lower-case. Use this for SSO/login paths. */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(?1)")
    Optional<User> findByEmailIgnoreCase(String email);

    /** Case-insensitive username lookup — IDEM may send only the sAMAccountName
     *  (e.g. "Someshwar.Phadatare") in the preferred_username claim. */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(?1)")
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByActiveTrue();
    List<User> findByRoleAndActiveTrue(UserRole role);

    @Query("SELECT DISTINCT u.team FROM User u WHERE u.team IS NOT NULL AND u.team <> '' ORDER BY u.team")
    List<String> findDistinctTeams();

    List<User> findByTeamAndRole(String team, UserRole role);

    /** Count members in a team */
    long countByTeamIdAndActiveTrue(UUID teamId);

    /** Find users by team ID */
    List<User> findByTeamIdAndActiveTrue(UUID teamId);

    /** Find users by team ID with specific role */
    List<User> findByTeamIdAndRoleAndActiveTrue(UUID teamId, UserRole role);
}
