package cft.rpe.legacy.s2s.support.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for S2S testing support lease calls. For non-production environments only.
 */
@ConditionalOnProperty(value = "idam.s2s-auth.testing-support.enabled", havingValue = "true", matchIfMissing = false)
@FeignClient(name = "rpetestingsupportapi", url = "${idam.s2s-auth.url}")
public interface RpeS2STestingSupportApi {

    /**
     * Microservice attribute name.
     */
    String MICROSERVICE = "microservice";

    /**
     * Convenience method to call lease for a single microservice.
     * @param serviceName S2S service name
     * @return S2S token
     */
    default String lease(String serviceName) {
        return lease(Map.of(MICROSERVICE, serviceName));
    }

    /**
     * S2S testing support lease.
     * @param body request body
     * @return S2S token
     */
    @PostMapping("/testing-support/lease")
    String lease(@RequestBody Map<String, String> body);

}
