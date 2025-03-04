package org.apereo.cas.services;

import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ShibbolethCompatiblePersistentIdGenerator;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.spring.ApplicationContextProvider;

import com.google.common.collect.ArrayListMultimap;
import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.io.Serial;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link BaseRegisteredService}.
 *
 * @author Marvin S. Addison
 * @since 3.4.12
 */
@Setter
@Tag("RegisteredService")
class RegisteredServiceTests {
    private static final long ID = 1000;

    private static final String SERVICE_ID = "test";

    private static final String DESCRIPTION = "test";

    private static final String SERVICEID = "serviceId";

    private static final String THEME = "theme";

    private static final String NAME = "name";

    private static final boolean ENABLED = false;

    private static final boolean SSO_ENABLED = false;

    private static final String ATTR_1 = "attr1";

    private static final String ATTR_2 = "attr2";

    private static final String ATTR_3 = "attr3";

    private final BaseRegisteredService baseService = new BaseRegisteredService() {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public void setServiceId(final String id) {
            serviceId = id;
        }

        @Override
        public boolean matches(final Service service) {
            return true;
        }

        @Override
        public boolean matches(final String serviceId) {
            return true;
        }
    };

    @BeforeEach
    public void beforeEach() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        ApplicationContextProvider.registerBeanIntoApplicationContext(applicationContext, CasConfigurationProperties.class,
            CasConfigurationProperties.class.getSimpleName());
        ApplicationContextProvider.holdApplicationContext(applicationContext);
    }

    @Test
    void verifyAllowToProxyIsFalseByDefault() {
        val service = new CasRegisteredService();
        assertFalse(service.getProxyPolicy().isAllowedToProxy());
    }

    @Test
    void verifySettersAndGetters() {
        prepareService();
        assertEquals(DESCRIPTION, baseService.getDescription());
        assertEquals(ENABLED, baseService.getAccessStrategy().isServiceAccessAllowed());
        assertEquals(ID, baseService.getId());
        assertEquals(NAME, baseService.getName());
        assertEquals(SERVICEID, baseService.getServiceId());
        assertEquals(SSO_ENABLED, baseService.getAccessStrategy().isServiceAccessAllowedForSso());
        assertEquals(THEME, baseService.getTheme());
        assertNotNull(baseService);
        assertEquals(baseService, baseService);
    }

    @Test
    void verifyServiceAttributeFilterAllAttributes() {
        prepareService();
        baseService.setAttributeReleasePolicy(new ReturnAllAttributeReleasePolicy());
        val p = mock(Principal.class);
        val map = new HashMap<String, List<Object>>();
        map.put(ATTR_1, List.of("value1"));
        map.put(ATTR_2, List.of("value2"));
        map.put(ATTR_3, Arrays.asList("v3", "v4"));
        when(p.getAttributes()).thenReturn(map);
        when(p.getId()).thenReturn("principalId");

        val context = RegisteredServiceAttributeReleasePolicyContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService(SERVICE_ID))
            .service(RegisteredServiceTestUtils.getService())
            .principal(p)
            .build();
        val attr = baseService.getAttributeReleasePolicy().getAttributes(context);
        assertEquals(attr.size(), map.size());
    }

    @Test
    void verifyServiceAttributeFilterAllowedAttributes() {
        prepareService();
        val policy = new ReturnAllowedAttributeReleasePolicy();
        policy.setAllowedAttributes(Arrays.asList(ATTR_1, ATTR_3));
        baseService.setAttributeReleasePolicy(policy);
        val p = mock(Principal.class);
        val map = new HashMap<String, List<Object>>();
        map.put(ATTR_1, List.of("value1"));
        map.put(ATTR_2, List.of("value2"));
        map.put(ATTR_3, Arrays.asList("v3", "v4"));
        when(p.getAttributes()).thenReturn(map);
        when(p.getId()).thenReturn("principalId");

        val context = RegisteredServiceAttributeReleasePolicyContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService(SERVICE_ID))
            .service(RegisteredServiceTestUtils.getService())
            .principal(p)
            .build();
        val attr = baseService.getAttributeReleasePolicy().getAttributes(context);
        assertEquals(2, attr.size());
        assertTrue(attr.containsKey(ATTR_1));
        assertTrue(attr.containsKey(ATTR_3));
    }

    @Test
    void verifyServiceAttributeFilterMappedAttributes() {
        prepareService();
        val policy = new ReturnMappedAttributeReleasePolicy();
        val mappedAttr = ArrayListMultimap.<String, Object>create();
        mappedAttr.put(ATTR_1, "newAttr1");
        policy.setAllowedAttributes(CollectionUtils.wrap(mappedAttr));
        baseService.setAttributeReleasePolicy(policy);
        val p = mock(Principal.class);
        val map = new HashMap<String, List<Object>>();
        map.put(ATTR_1, List.of("value1"));
        map.put(ATTR_2, List.of("value2"));
        map.put(ATTR_3, Arrays.asList("v3", "v4"));
        when(p.getAttributes()).thenReturn(map);
        when(p.getId()).thenReturn("principalId");

        val context = RegisteredServiceAttributeReleasePolicyContext.builder()
            .registeredService(RegisteredServiceTestUtils.getRegisteredService(SERVICE_ID))
            .service(RegisteredServiceTestUtils.getService())
            .principal(p)
            .build();
        val attr = baseService.getAttributeReleasePolicy().getAttributes(context);
        assertEquals(1, attr.size());
        assertTrue(attr.containsKey("newAttr1"));
    }

    @Test
    void verifyServiceEquality() {
        val svc1 = RegisteredServiceTestUtils.getRegisteredService(SERVICEID, false);
        val svc2 = RegisteredServiceTestUtils.getRegisteredService(SERVICEID, false);
        assertEquals(svc1, svc2);
    }

    @Test
    void verifyServiceWithInvalidIdStillHasTheSameIdAfterCallingMatches() {
        val invalidId = "***";
        val service = RegisteredServiceTestUtils.getRegisteredService(invalidId);
        service.matches("notRelevant");
        assertEquals(invalidId, service.getServiceId());
    }

    private void prepareService() {
        baseService.setUsernameAttributeProvider(
            new AnonymousRegisteredServiceUsernameAttributeProvider(new ShibbolethCompatiblePersistentIdGenerator("casrox")));
        baseService.setDescription(DESCRIPTION);
        baseService.setId(ID);
        baseService.setName(NAME);
        baseService.setServiceId(SERVICEID);
        baseService.setTheme(THEME);
        baseService.setAccessStrategy(new DefaultRegisteredServiceAccessStrategy(ENABLED, SSO_ENABLED));
    }
}
