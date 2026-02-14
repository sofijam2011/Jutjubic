package com.example.jutjubic.service;

import com.example.jutjubic.dto.AuthResponse;
import com.example.jutjubic.dto.LoginRequest;
import com.example.jutjubic.dto.RegisterRequest;
import com.example.jutjubic.model.User;
import com.example.jutjubic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Transactional
    public void register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Lozinke se ne poklapaju");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email je već registrovan");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Korisničko ime je već zauzeto");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setEnabled(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {

        if (loginAttemptService.isBlocked(ipAddress)) {
            throw new RuntimeException("Previše pokušaja prijave. Pokušajte ponovo za 1 minut.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            loginAttemptService.recordLoginAttempt(ipAddress, false);
            throw new RuntimeException("Neispravni kredencijali");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordLoginAttempt(ipAddress, false);
            throw new RuntimeException("Neispravni kredencijali");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordLoginAttempt(ipAddress, false);
            throw new RuntimeException("Neispravni kredencijali");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Nalog nije aktiviran. Proverite svoj email.");
        }

        loginAttemptService.recordLoginAttempt(ipAddress, true);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional
    public void verifyAccount(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Nevažeći verification token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token je istekao");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setTokenExpiryDate(null);

        userRepository.save(user);
    }
}
