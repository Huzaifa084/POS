package com.devaxiom.pos.repositories;

import com.devaxiom.pos.model.Users;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByName(String userName);

    @Query("SELECT u FROM Users u WHERE u.name LIKE %:query% OR u.email LIKE %:query%")
    public List<Users> searchUser(@Param("query") String query);

    boolean existsByEmail(@Email(message = "Enter a valid email") String email);
}
