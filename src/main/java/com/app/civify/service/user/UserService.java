package com.app.civify.service.user;

import com.app.civify.entity.User;
import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(Long id);
    User getCurrentUser();
    long countTotalUsers();
}
