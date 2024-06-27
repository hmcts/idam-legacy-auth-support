package cft.rpe.legacy.s2s.support;

import cft.rpe.legacy.s2s.support.api.RpeS2STestingSupportApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RpeS2STestingSupportAuthTokenGeneratorTest {

    @Mock
    RpeS2STestingSupportApi rpeS2STestingSupportApi;

    @InjectMocks
    RpeS2STestingSupportAuthTokenGenerator underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "serviceName", "test-service-name");
    }

    @Test
    void generate() {
        given(rpeS2STestingSupportApi.lease("test-service-name")).willReturn("test-token");
        assertEquals(underTest.generate(), "test-token");
    }
}