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
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exist: " + request.getEmail());
        }

        // Data
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

}
