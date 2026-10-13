package org.example.Controllers;

import org.example.Security.AccessCoin;
import org.example.Security.AccessCoinStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access-coins")
public class AccessCoinController {

    private final AccessCoinStore accessCoinStore;

    public AccessCoinController(AccessCoinStore accessCoinStore) {
        this.accessCoinStore = accessCoinStore;
    }

    @GetMapping
    public List<AccessCoin> list() {
        return accessCoinStore.list();
    }

    public record CreateCoinRequest(String name) {}

    @PostMapping
    public ResponseEntity<AccessCoin> create(
            @RequestParam(required = false) String name,
            @RequestBody(required = false) CreateCoinRequest request) {
        String coinName = request != null && request.name() != null ? request.name() : name;
        AccessCoin coin = accessCoinStore.create(coinName);
        return ResponseEntity.status(HttpStatus.CREATED).body(coin);
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable String id) {
        boolean revoked = accessCoinStore.revoke(id);
        if (!revoked) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
