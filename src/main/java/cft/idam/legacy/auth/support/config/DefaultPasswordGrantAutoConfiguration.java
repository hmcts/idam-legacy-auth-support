package cft.idam.legacy.auth.support.config;

import cft.idam.legacy.auth.support.PasswordGrantAuthorizedClientProvider;
import cft.idam.legacy.auth.support.PasswordGrantRequestInterceptor;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

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
     * Creates the auto-configuration populated from the application's password-grant properties.
     */
    public DefaultPasswordGrantAutoConfiguration() {
        // Spring injects the configured values after construction.
    }

    /**
     * Default password grant feign request interceptor.
     *
     * @param oauth2AuthorizedClientService from spring
     * @param clientRegistrationRepository  from spring
     * @param restClient HTTP client shared by password and refresh token requests
     * @return Password grant request interceptor.
     */
    @Bean
    public RequestInterceptor defaultPasswordGrantInterceptor(
            OAuth2AuthorizedClientService oauth2AuthorizedClientService,
            ClientRegistrationRepository clientRegistrationRepository,
            @Qualifier("legacyPasswordGrantRestClient") RestClient restClient) {
        log.info("idam-legacy-auth-support: Configured defaultPasswordGrantInterceptor "
                        + "for client reference: {}, endpoints: {}",
                clientRegistrationReference, passwordGrantEndpointRegex);
        return new PasswordGrantRequestInterceptor(
                clientRegistrationRepository.findByRegistrationId(clientRegistrationReference),
                passwordGrantAuthorizedClientManager(
                        oauth2AuthorizedClientService, clientRegistrationRepository, restClient),
                serviceAccountEmail,
                serviceAccountPassword,
                passwordGrantEndpointRegex
        );
    }

    /**
     * Creates the HTTP client used for legacy password grants and token refresh.
     *
     * <p>Service API calls use Feign, but token requests use RestClient to match Spring Security 7's
     * refresh-token client. Sharing this client lets both grant types use Spring's OAuth form and
     * token-response converters and error handling. It also keeps token requests outside Feign's
     * authorization interceptors, avoiding recursive attempts to obtain a token.
     *
     * @return client configured for OAuth form requests, token responses and errors
     */
    @Bean
    @ConditionalOnMissingBean(name = "legacyPasswordGrantRestClient")
    public RestClient legacyPasswordGrantRestClient() {
        return RestClient.builder()
                .configureMessageConverters(converters -> converters
                        .addCustomConverter(new FormHttpMessageConverter())
                        .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
    }

    private OAuth2AuthorizedClientManager passwordGrantAuthorizedClientManager(
            OAuth2AuthorizedClientService oauth2AuthorizedClientService,
            ClientRegistrationRepository clientRegistrationRepository, RestClient restClient) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
                        oauth2AuthorizedClientService);
        RestClientRefreshTokenTokenResponseClient refreshClient = new RestClientRefreshTokenTokenResponseClient();
        refreshClient.setRestClient(restClient);
        authorizedClientManager
                .setAuthorizedClientProvider(
                        OAuth2AuthorizedClientProviderBuilder.builder()
                                .provider(new PasswordGrantAuthorizedClientProvider(restClient))
                                .refreshToken(refresh -> refresh.accessTokenResponseClient(refreshClient)).build());
        authorizedClientManager.setContextAttributesMapper(systemUserCredentials());
        return authorizedClientManager;
    }

    private Function<OAuth2AuthorizeRequest, Map<String, Object>> systemUserCredentials() {
        return authorizeRequest -> {
            String username = authorizeRequest.getAttribute(PasswordGrantAuthorizedClientProvider.USERNAME);
            String password = authorizeRequest.getAttribute(PasswordGrantAuthorizedClientProvider.PASSWORD);
            if (username != null && password != null) {
                return Map.of(PasswordGrantAuthorizedClientProvider.USERNAME, username,
                        PasswordGrantAuthorizedClientProvider.PASSWORD, password);
            }
            return Collections.emptyMap();
        };
    }

}
