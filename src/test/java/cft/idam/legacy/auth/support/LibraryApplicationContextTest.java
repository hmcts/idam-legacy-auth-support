package cft.idam.legacy.auth.support;

import cft.rpe.legacy.s2s.support.RpeS2SRequestInterceptor;
import cft.rpe.legacy.s2s.support.api.RpeS2STestingSupportApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LibraryApplicationContextTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "idam.legacy.password-grant.registration-reference=test-client",
            "idam.legacy.password-grant.service-account.email-address=service@example.test",
            "idam.legacy.password-grant.service-account.password=test-password",
            "idam.legacy.password-grant.endpoint-regex=/protected/.*",
            "idam.s2s-auth.microservice=test-service",
            "idam.s2s-auth.totp_secret=test-secret",
            "idam.s2s-auth.endpoint-regex=/protected/.*",
            "idam.s2s-auth.url=http://s2s.example.test",
            "idam.s2s-auth.testing-support.enabled=true"
        })
class LibraryApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void createsPasswordGrantInterceptor() {
        assertThat(context.getBean("defaultPasswordGrantInterceptor"))
                .as("Password grant configuration should be discovered through auto-configuration imports")
                .isInstanceOf(PasswordGrantRequestInterceptor.class);
    }

    @Test
    void createsS2SInterceptor() {
        assertThat(context.getBean("rdServiceAuthorizationInterceptor"))
                .as("S2S configuration should create its request interceptor")
                .isInstanceOf(RpeS2SRequestInterceptor.class);
    }

    @Test
    void createsTestingSupportFeignClient() {
        assertThat(context.getBean(RpeS2STestingSupportApi.class))
                .as("The testing-support Feign client should be created without calling S2S")
                .isNotNull();
    }

    @Test
    void createsTestingSupportGenerator() {
        assertThat(context.getBeansOfType(AuthTokenGenerator.class))
                .as("Testing-support mode should create only its selected generator")
                .containsOnlyKeys("s2sTestingSupportAuthTokenGenerator");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("test-client")
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .authorizationGrantType(new AuthorizationGrantType("password"))
                    .tokenUri("http://idam.example.test/token")
                    .build());
        }

        @Bean
        OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
            return new InMemoryOAuth2AuthorizedClientService(repository);
        }
    }
}
