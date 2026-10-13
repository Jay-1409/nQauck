package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.dashboard.data-dir=${java.io.tmpdir}/pongpin-test")
class MainTests {

    @Test
    void contextLoads() {
    }

}
