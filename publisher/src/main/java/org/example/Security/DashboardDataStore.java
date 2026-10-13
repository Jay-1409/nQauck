package org.example.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.configuration.DashboardConfiguration;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Optional;

@Component
public class DashboardDataStore {

    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private final Path directory;
    private final ObjectMapper objectMapper;
    private SecretKey key;

    public DashboardDataStore(
            ObjectMapper objectMapper,
            org.springframework.core.env.Environment environment) {
        this.directory = Path.of(environment.getProperty("app.dashboard.data-dir",
                Path.of(System.getProperty("user.home"), ".pongpin").toString()));
        this.objectMapper = objectMapper;
    }

    public Path directory() {
        return directory;
    }

    public synchronized Optional<DashboardConfiguration> loadConfiguration() {
        Path file = directory.resolve("dashboard-config.enc");
        if (!Files.exists(file)) return Optional.empty();
        try {
            byte[] decrypted = decrypt(Files.readAllBytes(file));
            return Optional.of(objectMapper.readValue(decrypted, DashboardConfiguration.class));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read encrypted dashboard configuration", exception);
        }
    }

    public synchronized void saveConfiguration(DashboardConfiguration configuration) {
        try {
            Files.createDirectories(directory);
            setPermissions(directory, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
            byte[] encrypted = encrypt(objectMapper.writeValueAsBytes(configuration));
            writePrivateFile(directory.resolve("dashboard-config.enc"), encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save encrypted dashboard configuration", exception);
        }
    }

    public synchronized byte[] encrypt(byte[] data) throws Exception {
        byte[] nonce = new byte[NONCE_SIZE];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_SIZE, nonce));
        byte[] ciphertext = cipher.doFinal(data);
        byte[] encrypted = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, encrypted, 0, nonce.length);
        System.arraycopy(ciphertext, 0, encrypted, nonce.length, ciphertext.length);
        return encrypted;
    }

    public synchronized byte[] decrypt(byte[] encrypted) throws Exception {
        byte[] nonce = java.util.Arrays.copyOfRange(encrypted, 0, NONCE_SIZE);
        byte[] ciphertext = java.util.Arrays.copyOfRange(encrypted, NONCE_SIZE, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_SIZE, nonce));
        return cipher.doFinal(ciphertext);
    }

    private SecretKey encryptionKey() throws Exception {
        if (key != null) return key;
        Files.createDirectories(directory);
        setPermissions(directory, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
        Path keyFile = directory.resolve("config.key");
        if (Files.exists(keyFile)) {
            key = new javax.crypto.spec.SecretKeySpec(Files.readAllBytes(keyFile), "AES");
            return key;
        }
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        key = generator.generateKey();
        writePrivateFile(keyFile, key.getEncoded());
        return key;
    }

    public void writePrivateFile(Path destination, byte[] data) throws Exception {
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
            // shortcut: non-POSIX filesystems use default ACL, implement explicit Windows ACLs if cross-platform deployment requires it.
        }
    }

}
