package com.app.civify.mapper;

import java.util.List;

public interface CommonMapper<T, D> {

    D toDto(T entity);

    T toEntity(D dto);

    List<D> toDto(List<T> entityList);

    List<T> toEntity(List<D> dtoList);
}
