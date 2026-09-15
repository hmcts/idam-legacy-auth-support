package cft.idam.legacy.auth.support;

import cft.idam.legacy.auth.support.config.DefaultPasswordGrantAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootConfiguration
@EnableAutoConfiguration
class PasswordGrantTestApplication {

    @Bean
    ClientRegistrationRepository registrations() {
        ClientRegistration selected = ClientRegistration
                .withRegistrationId(PasswordGrantTestSupport.REGISTRATION)
                .clientId(PasswordGrantTestSupport.CLIENT_ID).clientSecret("secret&value")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .scope("openid", "profile").tokenUri(PasswordGrantTestSupport.TOKEN_URI).build();
        ClientRegistration other = ClientRegistration.withClientRegistration(selected)
                .registrationId("other-client").clientId("wrong-client").build();
        return new InMemoryClientRegistrationRepository(other, selected);
    }

    @Bean
    OAuth2AuthorizedClientService clients(ClientRegistrationRepository repository) {
        return new InMemoryOAuth2AuthorizedClientService(repository);
    }

    @Bean
    RestClient.Builder tokenClientBuilder() {
        return new DefaultPasswordGrantAutoConfiguration().legacyPasswordGrantRestClient().mutate();
    }

    @Bean
    MockRestServiceServer server(RestClient.Builder builder) {
        return MockRestServiceServer.bindTo(builder).build();
    }

    @Bean
    RestClient legacyPasswordGrantRestClient(RestClient.Builder builder, MockRestServiceServer server) {
        return builder.build();
    }
}
