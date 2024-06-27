package cft.idam.legacy.auth.support;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordGrantRequestInterceptorTest {

    @Mock
    ClientRegistration clientRegistration;

    @Mock
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Mock
    OAuth2AuthorizedClient oAuth2AuthorizedClient;

    @Mock
    OAuth2AccessToken oAuth2AccessToken;

    @Mock
    RequestTemplate requestTemplate;

    private PasswordGrantRequestInterceptor underTest;

    @BeforeEach
    public void setup() {
        given(clientRegistration.getClientId()).willReturn("test-client");
        underTest = new PasswordGrantRequestInterceptor(clientRegistration, oAuth2AuthorizedClientManager, "test-user",
                "test-pass", "/test-url");
    }

    @Test
    void applySuccess() {
        given(requestTemplate.url()).willReturn("/test-url");
        given(clientRegistration.getRegistrationId()).willReturn("test-reg");
        given(oAuth2AuthorizedClientManager.authorize(any())).willReturn(oAuth2AuthorizedClient);
        given(oAuth2AuthorizedClient.getAccessToken()).willReturn(oAuth2AccessToken);
        given(oAuth2AccessToken.getTokenValue()).willReturn("test-token");
        underTest.apply(requestTemplate);
        verify(requestTemplate).header(eq("Authorization"), eq("Bearer test-token"));
    }

    @Test
    void applyInvalidUrl() {
        given(requestTemplate.url()).willReturn("/invalid-url");
        underTest.apply(requestTemplate);
        verify(clientRegistration, never()).getRegistrationId();
        verify(oAuth2AuthorizedClientManager, never()).authorize(any());
    }

    @Test
    void applyNullUrl() {
        given(requestTemplate.url()).willReturn(null);
        underTest.apply(requestTemplate);
        verify(clientRegistration, never()).getRegistrationId();
        verify(oAuth2AuthorizedClientManager, never()).authorize(any());
    }

    @Test
    void applyNullClient() {
        given(requestTemplate.url()).willReturn("/test-url");
        given(clientRegistration.getRegistrationId()).willReturn("test-reg");
        given(oAuth2AuthorizedClientManager.authorize(any())).willReturn(null);
        try {
            underTest.apply(requestTemplate);
            fail();
        } catch (IllegalStateException ise) {
            assertEquals("password grant flow on test-reg failed, client is null", ise.getMessage());
        }
    }

}