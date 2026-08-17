package com.centerton.centerton;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "ai-chat.emergency-rules.path=classpath:rag/emergency_rules.json"
})
class CentertonApplicationTests {

    @Test
    void contextLoads() {
    }

}
