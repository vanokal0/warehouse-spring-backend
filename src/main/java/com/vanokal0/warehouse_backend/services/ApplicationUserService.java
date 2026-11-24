package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.ApplicationUser;
import com.vanokal0.warehouse_backend.repositories.ApplicationUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApplicationUserService {

    private final ApplicationUserRepository userRepository;

    @Autowired
    public ApplicationUserService(ApplicationUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<ApplicationUser> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(rawPassword));
    }
}
