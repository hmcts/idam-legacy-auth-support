package cft.rpe.legacy.s2s.support;

import cft.rpe.legacy.s2s.support.api.RpeS2STestingSupportApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

/**
 * AuthTokenGenerator that wraps S2S testing support lease calls. For non-production environments only.
 */
public class RpeS2STestingSupportAuthTokenGenerator implements AuthTokenGenerator {

    private final String serviceName;
    private final RpeS2STestingSupportApi rpeS2STestingSupportApi;

    /**
     * Constructor.
     * @param serviceName S2S microservice name.
     * @param rpeS2STestingSupportApi S2S testing support feign client.
     */
    public RpeS2STestingSupportAuthTokenGenerator(String serviceName, RpeS2STestingSupportApi rpeS2STestingSupportApi) {
        this.serviceName = serviceName;
        this.rpeS2STestingSupportApi = rpeS2STestingSupportApi;
    }

    @Override
    public String generate() {
        return rpeS2STestingSupportApi.lease(serviceName);
    }

}
