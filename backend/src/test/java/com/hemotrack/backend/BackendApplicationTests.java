package com.hemotrack.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
