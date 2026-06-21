package com.trs.security_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.trs.security_service.client.UserServiceClient;
import com.trs.security_service.data.AuthRequest;
import com.trs.security_service.data.AuthResponse;
import com.trs.security_service.data.UserRequest;
import com.trs.security_service.data.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(AuthRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        UserRequest userRequest = new UserRequest(
            request.getEmail(),
            hashedPassword
        );

        userServiceClient.createUser(userRequest);
        return "User registered successfully";
    }

    public AuthResponse login(AuthRequest request) {
        UserResponse user = userServiceClient.getUserByEmail(request.getEmail());

        if(user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        boolean passwordMatch = passwordEncoder.matches(
            request.getPassword(), 
            user.getPassword()
        );

        if(!passwordMatch) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail());
    }

}
