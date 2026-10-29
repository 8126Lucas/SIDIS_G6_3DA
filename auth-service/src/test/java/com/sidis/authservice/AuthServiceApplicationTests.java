package com.sidis.authservice;

import com.sidis.authservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        // The inherited setup verifies a successful HTTP login and a non-empty token.
    }
}
