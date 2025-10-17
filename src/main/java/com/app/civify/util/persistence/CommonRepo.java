package com.app.civify.util.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface CommonRepo<T extends Serializable, I> extends JpaRepository<T, I>, JpaSpecificationExecutor<T> {

}
