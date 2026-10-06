package org.example.Controllers;

import org.example.Security.DashboardAccountStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;

@Controller
public class InitialSetupController {

    private final DashboardAccountStore accountStore;
    private final UserDetailsManager users;
    private final PasswordEncoder passwordEncoder;

    public InitialSetupController(
            DashboardAccountStore accountStore,
            UserDetailsManager users,
            PasswordEncoder passwordEncoder) {
        this.accountStore = accountStore;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/setup")
    public String setupPage() {
        return accountStore.load().isPresent() ? "redirect:/login" : "redirect:/setup.html";
    }

    @PostMapping("/setup")
    @ResponseBody
    public ResponseEntity<String> createAccount(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword) {
        String cleanUsername = username.trim();
        if (accountStore.load().isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Dashboard setup is already complete.");
        }
        if (cleanUsername.isBlank() || cleanUsername.length() > 64) {
            return ResponseEntity.badRequest().body("Username must be between 1 and 64 characters.");
        }
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            return ResponseEntity.badRequest().body("Password must be 12–72 bytes long.");
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match.");
        }
        try {
            var account = accountStore.create(cleanUsername, password, passwordEncoder);
            users.createUser(User.withUsername(account.username())
                    .password(account.passwordHash())
                    .roles("USER")
                    .build());
            return ResponseEntity.status(HttpStatus.CREATED).body("Dashboard account created.");
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Dashboard setup is already complete.");
        }
    }
}
