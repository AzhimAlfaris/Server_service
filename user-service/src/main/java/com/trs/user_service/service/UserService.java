package com.trs.user_service.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.trs.user_service.data.UserRequest;
import com.trs.user_service.model.User;
import com.trs.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User creatUser(UserRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if(userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exist: " + normalizedEmail);
        }

        // Data
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(request.getPassword());

        return userRepository.save(user);
    }

    public User updatePassword(UserRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + normalizedEmail));

        user.setPassword(request.getPassword());
        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public Boolean userExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

}
