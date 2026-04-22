package com.unityeats.service;

import com.unityeats.exception.BusinessException;
import com.unityeats.model.Role;
import com.unityeats.model.User;
import com.unityeats.repository.UserRepository;
import com.unityeats.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public User register(User request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict(
                "An account with email '" + request.getEmail() + "' already exists. Please login instead."
            );
        }

        if ((request.getRole() == Role.RESTAURANT || request.getRole() == Role.NGO)
                && (request.getOrganizationName() == null || request.getOrganizationName().isBlank())) {
            throw BusinessException.badRequest(
                "Organization name is required for " + request.getRole() + " accounts."
            );
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .organizationName(request.getOrganizationName() != null ? request.getOrganizationName().trim() : null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role {}", savedUser.getEmail(), savedUser.getRole());

        String token = jwtUtils.generateToken(savedUser);
        savedUser.setToken(token);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User login(User request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {} ({})", user.getEmail(), user.getRole());

        String token = jwtUtils.generateToken(user);
        user.setToken(token);

        return user;
    }
}
