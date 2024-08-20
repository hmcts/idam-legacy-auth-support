package cft.idam.legacy.auth.support.config;

import cft.idam.legacy.auth.support.PasswordGrantRequestInterceptor;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Default Password Grant Auto Configuration. Uses the Spring Security OAuth2 Client Registrations
 * for the calls to perform the password grant, so the registration-reference value needs to match
 * one of the oauth2 registrations defined in the spring security part of application.yaml.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "idam.legacy.password-grant", name = "registration-reference")
public class DefaultPasswordGrantAutoConfiguration {

    @Value("${idam.legacy.password-grant.registration-reference}")
    private String clientRegistrationReference;

    @Value("${idam.legacy.password-grant.service-account.email-address}")
    private String serviceAccountEmail;

    @Value("${idam.legacy.password-grant.service-account.password}")
    private String serviceAccountPassword;

    @Value("${idam.legacy.password-grant.endpoint-regex}")
    private String passwordGrantEndpointRegex;

    /**
     * Default password grant feign request interceptor.
     *
     * @param oauth2AuthorizedClientService from spring
     * @param clientRegistrationRepository  from spring
     * @return Password grant request interceptor.
     */
    @Bean
    public RequestInterceptor defaultPasswordGrantInterceptor(
            OAuth2AuthorizedClientService oauth2AuthorizedClientService,
            ClientRegistrationRepository clientRegistrationRepository) {
        log.info("idam-legacy-auth-support: Configured defaultPasswordGrantInterceptor "
                        + "for client reference: {}, endpoints: {}",
                clientRegistrationReference, passwordGrantEndpointRegex);
        return new PasswordGrantRequestInterceptor(
                clientRegistrationRepository.findByRegistrationId(clientRegistrationReference),
                passwordGrantAuthorizedClientManager(oauth2AuthorizedClientService, clientRegistrationRepository),
                serviceAccountEmail,
                serviceAccountPassword,
                passwordGrantEndpointRegex
        );
    }

    private OAuth2AuthorizedClientManager passwordGrantAuthorizedClientManager(
            OAuth2AuthorizedClientService oauth2AuthorizedClientService,
            ClientRegistrationRepository clientRegistrationRepository) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
                        oauth2AuthorizedClientService);
        authorizedClientManager
                .setAuthorizedClientProvider(
                        OAuth2AuthorizedClientProviderBuilder.builder()
                                .password()
                                .refreshToken().build());
        authorizedClientManager.setContextAttributesMapper(systemUserCredentials());
        return authorizedClientManager;
    }

    private Function<OAuth2AuthorizeRequest, Map<String, Object>> systemUserCredentials() {
        return authorizeRequest -> {
            String username = authorizeRequest.getAttribute(OAuth2ParameterNames.USERNAME);
            String password = authorizeRequest.getAttribute(OAuth2ParameterNames.PASSWORD);
            if (username != null && password != null) {
                return Map.of(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username,
                        OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
            }
            return Collections.emptyMap();
        };
    }

}
