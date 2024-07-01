package cft.rpe.legacy.s2s.support.config;

import cft.rpe.legacy.s2s.support.RpeS2SRequestInterceptor;
import cft.rpe.legacy.s2s.support.RpeS2STestingSupportAuthTokenGenerator;
import cft.rpe.legacy.s2s.support.api.RpeS2STestingSupportApi;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;
import uk.gov.hmcts.reform.authorisation.generators.AutorefreshingJwtAuthTokenGenerator;

/**
 * RPE S2S Auto Configuration.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "idam.s2s-auth", name = "endpoint-regex")
@EnableFeignClients(basePackageClasses = RpeS2STestingSupportApi.class)
public class RpeS2SAutoConfiguration {

    @Value("${idam.s2s-auth.microservice}")
    private String s2sServiceName;

    @Value("${idam.s2s-auth.totp_secret}")
    private String s2sServiceSecret;

    @Value("${idam.s2s-auth.endpoint-regex}")
    private String s2sEndpointRegex;

    /**
     * Configure primary real S2S generator.
     *
     * @param serviceAuthorisationApi S2S Api
     * @return Real S2S auth token generator.
     */
    @ConditionalOnProperty(value = "idam.s2s-auth.testing-support.enabled", havingValue = "false",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    @Primary
    @Bean
    public AuthTokenGenerator s2sAuthTokenGenerator(ServiceAuthorisationApi serviceAuthorisationApi) {
        log.info("idam-legacy-auth-support: Configured s2sAuthTokenGenerator for service: {}", s2sServiceName);
        return AuthTokenGeneratorFactory.createDefaultGenerator(
                s2sServiceSecret,
                s2sServiceName,
                serviceAuthorisationApi
        );
    }

    /**
     * Configure Test S2s Auth Token Generator.
     *
     * @param rpeS2STestingSupportApi rpe testing support feign api
     * @return Test S2S auth token generator.
     */
    @ConditionalOnProperty(value = "idam.s2s-auth.testing-support.enabled", havingValue = "true",
            matchIfMissing = false)
    @ConditionalOnMissingBean
    @Bean
    public AuthTokenGenerator s2sTestingSupportAuthTokenGenerator(RpeS2STestingSupportApi rpeS2STestingSupportApi) {
        log.info("idam-legacy-auth-support: Configured s2sTestingSupportAuthTokenGenerator for service: {}",
                s2sServiceName);
        AuthTokenGenerator atg = new RpeS2STestingSupportAuthTokenGenerator(s2sServiceName, rpeS2STestingSupportApi);
        return new AutorefreshingJwtAuthTokenGenerator(atg);
    }

    /**
     * Configure Feign interceptor to include S2S custom header.
     *
     * @param authTokenGenerator S2S Auth Token Generator.
     * @return feign s2s request interceptor.
     */
    @ConditionalOnProperty(prefix = "idam.s2s-auth", name = "endpoint-regex")
    @Bean
    public RequestInterceptor rdServiceAuthorizationInterceptor(AuthTokenGenerator authTokenGenerator) {
        log.info(
                "idam-legacy-auth-support: Configured rdServiceAuthorizationInterceptor for service: {}, endpoints: {}",
                s2sServiceName, s2sEndpointRegex);
        return new RpeS2SRequestInterceptor(authTokenGenerator, s2sEndpointRegex);
    }

}
