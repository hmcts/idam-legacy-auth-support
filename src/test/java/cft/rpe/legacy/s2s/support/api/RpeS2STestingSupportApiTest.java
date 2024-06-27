package cft.rpe.legacy.s2s.support.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpeS2STestingSupportApiTest {

    @Captor
    ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

    @Test
    void testLeaseByServiceName() {
        RpeS2STestingSupportApi underTest = mock(RpeS2STestingSupportApi.class);
        when(underTest.lease("test-service-name")).thenCallRealMethod();
        underTest.lease("test-service-name");
        verify(underTest).lease(mapArgumentCaptor.capture());
        Map<String, String> callMap = mapArgumentCaptor.getValue();
        assertEquals(1, callMap.size());
        assertEquals("test-service-name", callMap.get("microservice"));
    }
}