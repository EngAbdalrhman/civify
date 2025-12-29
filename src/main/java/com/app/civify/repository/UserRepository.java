package com.app.civify.repository;

import com.app.civify.entity.User;
import com.app.civify.util.persistence.CommonRepo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CommonRepo<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Get only extraTokens (wallet balance) for a user.
     * Optimized query - avoids loading the entire User entity.
     */
    @Query("SELECT u.extraTokens FROM User u WHERE u.id = :userId")
    Optional<Integer> findExtraTokensByUserId(@Param("userId") Long userId);

    /**
     * Update extraTokens directly with a single UPDATE query.
     * Returns the number of rows affected.
     */
    @Modifying
    @Query("UPDATE User u SET u.extraTokens = :tokens WHERE u.id = :userId")
    int updateExtraTokens(@Param("userId") Long userId, @Param("tokens") Integer tokens);
}
