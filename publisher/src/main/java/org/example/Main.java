package org.example;

import org.example.Security.DashboardAccountStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@Controller
public class Main {

    private final DashboardAccountStore accountStore;

    public Main(DashboardAccountStore accountStore) {
        this.accountStore = accountStore;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @GetMapping("/")
    public String home() {
        return accountStore.load().isPresent() ? "redirect:/index.html" : "redirect:/setup";
    }
}
