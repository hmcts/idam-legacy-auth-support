package cft.rpe.legacy.s2s.support;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RpeS2SRequestInterceptorTest {

    @Mock
    AuthTokenGenerator authTokenGenerator;

    @Mock
    RequestTemplate requestTemplate;

    private RpeS2SRequestInterceptor underTest;

    @BeforeEach
    public void setup() {
        underTest = new RpeS2SRequestInterceptor(authTokenGenerator, "/test-url");
    }

    @Test
    void applyHandleUrl() {
        given(requestTemplate.url()).willReturn("/test-url");
        given(authTokenGenerator.generate()).willReturn("test-token");
        underTest.apply(requestTemplate);
        verify(requestTemplate).header(eq("ServiceAuthorization"), eq("Bearer test-token"));
    }

    @Test
    void applyHandleUrlWithTokenStartingWithBearer() {
        given(requestTemplate.url()).willReturn("/test-url");
        given(authTokenGenerator.generate()).willReturn("Bearer test-token");
        underTest.apply(requestTemplate);
        verify(requestTemplate).header(eq("ServiceAuthorization"), eq("Bearer test-token"));
    }

    @Test
    void applyInvalidUrl() {
        given(requestTemplate.url()).willReturn("/invalid-url");
        underTest.apply(requestTemplate);
        verify(authTokenGenerator, never()).generate();
        verify(requestTemplate, never()).header(eq("ServiceAuthorization"), anyString());
    }

    @Test
    void applyHandleUrlWithNullUrl() {
        given(requestTemplate.url()).willReturn(null);
        underTest.apply(requestTemplate);
        verify(authTokenGenerator, never()).generate();
        verify(requestTemplate, never()).header(eq("ServiceAuthorization"), anyString());
    }

    @Test
    void applyHandleUrlWithNullToken() {
        given(requestTemplate.url()).willReturn("/test-url");
        given(authTokenGenerator.generate()).willReturn(null);
        underTest.apply(requestTemplate);
        verify(requestTemplate).header("ServiceAuthorization", "Bearer null");
    }
}