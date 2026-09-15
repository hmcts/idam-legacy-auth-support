package cft.idam.legacy.auth.support.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DefaultPasswordGrantAutoConfigurationTest {

    @Test
    void remainsDisabledWithoutRegistrationReference() {
        assertDoesNotThrow(() -> new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DefaultPasswordGrantAutoConfiguration.class))
                .run(context -> assertThat(context).as("Password grants should remain opt-in")
                        .doesNotHaveBean("defaultPasswordGrantInterceptor")
                        .doesNotHaveBean("legacyPasswordGrantRestClient")),
                "Disabled password grants should not require OAuth client or service-account configuration");
    }
}
