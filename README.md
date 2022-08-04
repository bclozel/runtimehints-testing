# RuntimeHints Java Agent

As of Spring Framework 6.0, the new `RuntimeHints` API helps you to contribute hints for runtime reflection, resources and proxies with GraalVM native.
It can be hard to find out about the required hints without compilingan application to native and seeing it fail.

The new `spring-core-test` module ships a Java agent that will help you with that.
This agent records all method invocations that are related to such hints and helps you to assert that a given `RuntimeHints` instance
covers all recorded invocations.

## Testing runtime hints with the agent

Let's consider a piece of infrastructure for which we'd like to test the hints we're contributing during the AOT phase.

```java
public class SampleReflection {

	private final Log logger = LogFactory.getLog(SampleReflection.class);

	public void performReflection() {
		try {
			Class<?> springVersion = ClassUtils.forName("org.springframework.core.SpringVersion", null);
			Method getVersion = ClassUtils.getMethod(springVersion, "getVersion");
			String version = (String) getVersion.invoke(null);
			logger.info("Spring version:" + version);
		}
		catch (Exception exc) {
			  logger.error("reflection failed", exc);
		}
	}
}
```

We can then write a Java test (no native compilation required!) that checks our contributed hints:

```java
// this annotation conditions the execution of tests only if the agent is loaded in the current JVM
// it also tags tests with the "RuntimeHints" JUnit tag
@EnabledIfRuntimeHintsAgent
class SampleReflectionRuntimeHintsTests {

	@Test
	void shouldRegisterReflectionHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		// Call a RuntimeHintsRegistrar that contributes hints like:
		runtimeHints.reflection().registerType(SpringVersion.class, typeHint -> {
			typeHint.withMethod("getVersion", List.of(), methodHint -> methodHint.withMode(ExecutableMode.INVOKE));
		});

		// Invoke the relevant piece of code we want to test within a recording lambda
		RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.performReflection();
		});
		// assert that the recorded invocations are covered by the contributed hints
		assertThat(invocations).match(runtimeHints);
	}

}
```

If you forgot to contribute a hint, the test will fail and give some details on the invocation:

```
Aug 04, 2022 7:06:48 PM io.spring.runtimehintstesting.SampleReflection performReflection
INFO: Spring version:6.0.0-SNAPSHOT

Missing <"ReflectionHints"> for invocation <java.lang.Class#forName>
with arguments ["org.springframework.core.SpringVersion",
    false,
    jdk.internal.loader.ClassLoaders$AppClassLoader@251a69d7].
Stacktrace:
<"org.springframework.util.ClassUtils#forName, Line 284
io.spring.runtimehintstesting.SampleReflection#performReflection, Line 19
io.spring.runtimehintstesting.SampleReflectionRuntimeHintsTests#lambda$shouldRegisterReflectionHints$0, Line 25
```

## Gradle Configuration

This project shows how to configure the RuntimeHints Java agent for your tests.
The strategy chosen here is to configure a new `:runtimeHintsTest` task that only runs tests tagged `"RuntimeHints`.

This allows to load the Java agent only for the relevant tests and avoid polluting other tests.
You can run those with `./gradlew :runtimeHintsTest` and they are included with `./gradlew :check`.

The agent itself can be configured to instrument some packages (by default, only `org.springframework` is instrumented).
The Spring Framework project provides a Gradle DSL extension for this, copied in this project.
You'll find more details in the [Spring Framework buildSrc README](https://github.com/spring-projects/spring-framework/blob/main/buildSrc/README.md).

## Maven Configuration

With Maven, this project also shows how to configure the Java agent in an isolated test suite.
For this, a specific profile configures surefire to run the test runtimehints test suite with `./mvnw test -P runtimehints`. 