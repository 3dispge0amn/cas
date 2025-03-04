package org.apereo.cas.support.saml.web.support;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.support.saml.authentication.principal.SamlServiceFactory;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.web.support.DefaultArgumentExtractor;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Scott Battaglia
 * @since 3.1
 */
@Tag("Web")
class WebUtilTests {

    @Test
    void verifyFindService() {
        val casArgumentExtractor =
            new DefaultArgumentExtractor(new WebApplicationServiceFactory());
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "test");

        val service = HttpRequestUtils.getService(List.of(casArgumentExtractor), request);

        assertNotNull(service);
        assertEquals("test", service.getId());
    }

    @Test
    void verifyFoundNoService() {
        val casArgumentExtractor = new DefaultArgumentExtractor(new SamlServiceFactory());
        val request = new MockHttpServletRequest();
        request.setParameter(CasProtocolConstants.PARAMETER_SERVICE, "test");
        val service = HttpRequestUtils.getService(List.of(casArgumentExtractor), request);
        assertNull(service);
    }
}
