package com.app.civify.util.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@NoRepositoryBean
public interface CommonRepo<T extends Serializable, I> extends JpaRepository<T, I>, JpaSpecificationExecutor<T> {

    @SuppressWarnings("unchecked")
    default Class<T> getEntityClass() {
        Class<?> entityClass = resolveEntityClass(getClass());
        if (entityClass == null) {
            throw new IllegalStateException("Unable to resolve entity class for repository " + getClass().getName());
        }
        return (Class<T>) entityClass;
    }

    static Class<?> resolveEntityClass(Class<?> repositoryClass) {
        if (repositoryClass == null) {
            return null;
        }

        Class<?> resolved = resolveEntityClass((Type) repositoryClass);
        if (resolved != null) {
            return resolved;
        }

        for (Class<?> interfaceClass : repositoryClass.getInterfaces()) {
            resolved = resolveEntityClass(interfaceClass);
            if (resolved != null) {
                return resolved;
            }
        }

        return resolveEntityClass(repositoryClass.getSuperclass());
    }

    private static Class<?> resolveEntityClass(Type candidate) {
        if (candidate == null) {
            return null;
        }

        if (candidate instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass) {
                if (JpaRepository.class.isAssignableFrom(rawClass)
                        || JpaSpecificationExecutor.class.isAssignableFrom(rawClass)) {
                    Type entityType = parameterizedType.getActualTypeArguments()[0];
                    if (entityType instanceof Class<?> entityClass) {
                        return entityClass;
                    }
                }

                Class<?> resolved = resolveEntityClass(rawClass);
                if (resolved != null) {
                    return resolved;
                }
            }
        } else if (candidate instanceof Class<?> candidateClass) {
            for (Type genericInterface : candidateClass.getGenericInterfaces()) {
                Class<?> resolved = resolveEntityClass(genericInterface);
                if (resolved != null) {
                    return resolved;
                }
            }
            return resolveEntityClass(candidateClass.getGenericSuperclass());
        }

        return null;
    }
}

