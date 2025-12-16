package com.app.civify.repository;

import com.app.civify.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Group> {
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.permissions WHERE g.id = :id")
    Optional<Group> findByIdWithPermissions(@Param("id") Long id);
    Optional<Group> findByName(String name);
    Optional<Group> findByIsDefaultTrue();
}
