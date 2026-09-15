package cft.rpe.legacy.s2s.support.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class RpeS2SAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class, RpeS2SAutoConfiguration.class))
            .withBean(ServiceAuthorisationApi.class, () -> mock(ServiceAuthorisationApi.class))
            .withPropertyValues("idam.s2s-auth.microservice=test-service",
                    "idam.s2s-auth.totp_secret=test-secret", "idam.s2s-auth.url=http://s2s.example.test");

    @Test
    void remainsDisabledWithoutEndpointPattern() {
        assertDoesNotThrow(() -> runner.run(context -> {
            assertThat(context).as("S2S support should be opt-in")
                .doesNotHaveBean(AuthTokenGenerator.class);
        }), "The context should start and satisfy the bean assertions");
    }

    @Test
    void defaultsToRealGenerator() {
        assertDoesNotThrow(() -> runner.withPropertyValues("idam.s2s-auth.endpoint-regex=/protected/.*")
                .run(context -> {
                    assertThat(context.getBeansOfType(AuthTokenGenerator.class))
                        .as("Omitting testing-support.enabled should select the real generator")
                        .containsOnlyKeys("s2sAuthTokenGenerator");
                }), "The context should start and satisfy the bean assertions");
    }

    @Test
    void explicitFalseSelectsRealGenerator() {
        assertDoesNotThrow(() -> runner.withPropertyValues("idam.s2s-auth.endpoint-regex=/protected/.*",
                        "idam.s2s-auth.testing-support.enabled=false")
                .run(context -> {
                    assertThat(context.getBeansOfType(AuthTokenGenerator.class))
                        .as("Disabling testing support should select the real generator")
                        .containsOnlyKeys("s2sAuthTokenGenerator");
                }), "The context should start and satisfy the bean assertions");
    }

    @Test
    void customGeneratorOverridesDefault() {
        AuthTokenGenerator custom = mock(AuthTokenGenerator.class);
        assertDoesNotThrow(() -> runner.withPropertyValues("idam.s2s-auth.endpoint-regex=/protected/.*")
                .withBean("customGenerator", AuthTokenGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context.getBean(AuthTokenGenerator.class))
                        .as("A consumer-provided generator should take precedence")
                        .isSameAs(custom);
                }), "The context should start and satisfy the bean assertions");
    }

    @Test
    void customGeneratorOverridesTestingSupport() {
        AuthTokenGenerator custom = mock(AuthTokenGenerator.class);
        assertDoesNotThrow(() -> runner.withPropertyValues("idam.s2s-auth.endpoint-regex=/protected/.*",
                        "idam.s2s-auth.testing-support.enabled=true")
                .withBean("customGenerator", AuthTokenGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context.getBean(AuthTokenGenerator.class))
                        .as("A consumer-provided generator should also override testing support")
                        .isSameAs(custom);
                }), "The context should start and satisfy the bean assertions");
    }
}
