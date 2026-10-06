package org.example.Security;

import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;

@Component
public class DashboardAccountStore {

    private final Path directory;

    public DashboardAccountStore(Environment environment) {
        this.directory = Path.of(environment.getProperty("app.dashboard.data-dir",
                Path.of(System.getProperty("user.home"), ".pongpin").toString()));
    }

    public synchronized Optional<Account> load() {
        Path file = directory.resolve("dashboard-user.properties");
        try {
            if (!Files.exists(file)) return Optional.empty();
            Properties properties = new Properties();
            try (var input = Files.newInputStream(file)) {
                properties.load(input);
            }
            String username = properties.getProperty("username");
            String passwordHash = properties.getProperty("passwordHash");
            if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank()) {
                throw new IllegalStateException("Saved dashboard account is incomplete");
            }
            return Optional.of(new Account(username, passwordHash));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read dashboard account", exception);
        }
    }

    public synchronized Account create(String username, String password, PasswordEncoder encoder) {
        if (load().isPresent()) throw new IllegalStateException("Dashboard account is already configured");
        try {
            Files.createDirectories(directory);
            setPermissions(directory, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
            String passwordHash = encoder.encode(password);
            Properties properties = new Properties();
            properties.setProperty("username", username);
            properties.setProperty("passwordHash", passwordHash);
            var output = new java.io.ByteArrayOutputStream();
            properties.store(output, "Pongpin dashboard account; password is BCrypt hashed");
            writePrivateFile(directory.resolve("dashboard-user.properties"), output.toByteArray());
            return new Account(username, passwordHash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save dashboard account", exception);
        }
    }

    private void writePrivateFile(Path destination, byte[] data) throws Exception {
        Path temporary = Files.createTempFile(directory, ".pongpin-", ".tmp");
        try {
            setPermissions(temporary, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            Files.write(temporary, data);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            setPermissions(destination, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void setPermissions(Path path, EnumSet<PosixFilePermission> permissions) throws Exception {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // ponytail: non-POSIX filesystems rely on the current user's default ACL.
        }
    }

    public record Account(String username, String passwordHash) {}
}
