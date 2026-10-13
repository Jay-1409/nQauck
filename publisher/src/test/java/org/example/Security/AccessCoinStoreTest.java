package org.example.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCoinStoreTest {

    @TempDir
    Path tempDir;

    private AccessCoinStore coinStore;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.dashboard.data-dir", tempDir.toString());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DashboardDataStore dataStore = new DashboardDataStore(objectMapper, env);
        coinStore = new AccessCoinStore(dataStore, objectMapper);
    }

    @Test
    void createsListsAndValidatesCoins() {
        assertTrue(coinStore.list().isEmpty());
        assertFalse(coinStore.isValid("ac_nonexistent"));

        AccessCoin coin = coinStore.create("Production Key");

        assertNotNull(coin.id());
        assertEquals("Production Key", coin.name());
        assertTrue(coin.token().startsWith("ac_"));
        assertFalse(coin.revoked());

        List<AccessCoin> list = coinStore.list();
        assertEquals(1, list.size());
        assertEquals(coin.token(), list.getFirst().token());

        assertTrue(coinStore.isValid(coin.token()));
        assertFalse(coinStore.isValid("invalid-token"));

        boolean revoked = coinStore.revoke(coin.id());
        assertTrue(revoked);

        assertFalse(coinStore.isValid(coin.token()));
        assertTrue(coinStore.list().getFirst().revoked());
    }

    @Test
    void persistsEncryptedCoinsAcrossInstances() {
        AccessCoin coin = coinStore.create("Billing Service");

        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.dashboard.data-dir", tempDir.toString());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DashboardDataStore reloadedDataStore = new DashboardDataStore(objectMapper, env);
        AccessCoinStore reloadedStore = new AccessCoinStore(reloadedDataStore, objectMapper);

        List<AccessCoin> list = reloadedStore.list();
        assertEquals(1, list.size());
        assertEquals(coin.id(), list.getFirst().id());
        assertEquals("Billing Service", list.getFirst().name());
        assertTrue(reloadedStore.isValid(coin.token()));
    }
}
