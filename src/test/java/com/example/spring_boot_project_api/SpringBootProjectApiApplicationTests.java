package com.example.spring_boot_project_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
class SpringBootProjectApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
