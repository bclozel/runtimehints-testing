package io.spring.runtimehintstesting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleTests {

	@Test
	void testNotInstrumented() {
		assertThat(true).isTrue();
	}
}
