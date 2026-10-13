package org.example.Security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class AccessCoinStore {

    private final DashboardDataStore dataStore;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Path storageFile;

    public AccessCoinStore(DashboardDataStore dataStore, ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
        this.storageFile = dataStore.directory().resolve("access-coins.enc");
    }

    public synchronized List<AccessCoin> list() {
        if (!Files.exists(storageFile)) {
            return List.of();
        }
        try {
            byte[] decrypted = dataStore.decrypt(Files.readAllBytes(storageFile));
            List<AccessCoin> coins = objectMapper.readValue(decrypted, new TypeReference<>() {});
            return coins == null ? List.of() : List.copyOf(coins);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read encrypted access coins", exception);
        }
    }

    public synchronized AccessCoin create(String name) {
        String coinName = (name == null || name.isBlank()) ? "Default Coin" : name.trim();
        String id = UUID.randomUUID().toString();
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String token = "ac_" + HexFormat.of().formatHex(randomBytes);
        AccessCoin newCoin = new AccessCoin(id, coinName, token, Instant.now(), false);

        List<AccessCoin> current = new ArrayList<>(list());
        current.add(newCoin);
        save(current);
        return newCoin;
    }

    public synchronized boolean revoke(String id) {
        if (id == null || id.isBlank()) return false;
        List<AccessCoin> current = list();
        boolean found = false;
        List<AccessCoin> updated = new ArrayList<>(current.size());
        for (AccessCoin coin : current) {
            if (Objects.equals(coin.id(), id)) {
                updated.add(new AccessCoin(coin.id(), coin.name(), coin.token(), coin.createdAt(), true));
                found = true;
            } else {
                updated.add(coin);
            }
        }
        if (found) {
            save(updated);
        }
        return found;
    }

    public synchronized boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String cleanToken = token.trim();
        return list().stream()
                .anyMatch(coin -> !coin.revoked() && Objects.equals(coin.token(), cleanToken));
    }

    private void save(List<AccessCoin> coins) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(coins);
            byte[] encrypted = dataStore.encrypt(jsonBytes);
            dataStore.writePrivateFile(storageFile, encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save encrypted access coins", exception);
        }
    }
}
