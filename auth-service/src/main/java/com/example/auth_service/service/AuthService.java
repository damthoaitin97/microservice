package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.repository.RoleRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;

        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByUsername(request.getUsername())) {
                        return AuthResponse.builder()
                                        .message("Error: Username is already taken!")
                                        .build();
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        return AuthResponse.builder()
                                        .message("Error: Email is already in use!")
                                        .build();
                }

                // Create new user's account
                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .build();

                Set<Role> roles = new HashSet<>();

                Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);

                user.setRoles(roles);

                String jwt = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                user.setRefreshToken(refreshToken);
                user.setRefreshTokenExpiryDate(jwtService.extractExpiration(refreshToken));
                userRepository.save(user);

                return AuthResponse.builder()
                                .accessToken(jwt)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .message("User registered successfully!")
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                User user = (User) authentication.getPrincipal();

                String jwt = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                user.setRefreshToken(refreshToken);
                user.setRefreshTokenExpiryDate(jwtService.extractExpiration(refreshToken));
                userRepository.save(user);

                return AuthResponse.builder()
                                .accessToken(jwt)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .message("User logged in successfully!")
                                .build();
        }

        public AuthResponse refreshToken(String refreshToken) {
                String username = jwtService.extractUsername(refreshToken);
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (!jwtService.isTokenValid(refreshToken, user) || !user.getRefreshToken().equals(refreshToken)
                                || user.getRefreshTokenExpiryDate().before(new Date())) {
                        throw new RuntimeException("Invalid or expired refresh token");
                }

                String newAccessToken = jwtService.generateToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                user.setRefreshToken(newRefreshToken);
                user.setRefreshTokenExpiryDate(jwtService.extractExpiration(newRefreshToken));
                userRepository.save(user);

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .tokenType("Bearer")
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .message("Token refreshed successfully!")
                                .build();
        }
}