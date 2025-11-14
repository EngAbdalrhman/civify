package com.app.civify.repository;

import com.app.civify.entity.User;
import com.app.civify.util.persistence.CommonRepo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CommonRepo<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
