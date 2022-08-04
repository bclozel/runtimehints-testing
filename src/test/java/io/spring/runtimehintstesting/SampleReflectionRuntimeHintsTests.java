package io.spring.runtimehintstesting;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.aot.test.agent.RuntimeHintsRecorder;
import org.springframework.core.SpringVersion;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfRuntimeHintsAgent
class SampleReflectionRuntimeHintsTests {

	@Test
	void shouldRegisterReflectionHints() {
		RuntimeHints runtimeHints = new RuntimeHints();

		runtimeHints.reflection().registerType(SpringVersion.class, typeHint -> {
			typeHint.withMethod("getVersion", List.of(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE));
		});
		RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.performReflection();
		});
		assertThat(invocations).match(runtimeHints);
	}

}