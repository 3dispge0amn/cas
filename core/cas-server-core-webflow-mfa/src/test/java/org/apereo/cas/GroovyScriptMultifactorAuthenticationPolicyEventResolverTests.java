package org.apereo.cas;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.mfa.TestMultifactorAuthenticationProvider;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.apereo.inspektr.common.web.ClientInfo;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.binding.expression.support.LiteralExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.engine.Transition;
import org.springframework.webflow.engine.support.DefaultTargetStateResolver;
import org.springframework.webflow.engine.support.DefaultTransitionCriteria;
import org.springframework.webflow.test.MockRequestContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link GroovyScriptMultifactorAuthenticationPolicyEventResolverTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("GroovyAuthentication")
@TestPropertySource(properties = "cas.authn.mfa.groovy-script.location=classpath:GroovyMfaResolver.groovy")
@Import(GroovyScriptMultifactorAuthenticationPolicyEventResolverTests.GroovyMultifactorTestConfiguration.class)
class GroovyScriptMultifactorAuthenticationPolicyEventResolverTests extends BaseCasWebflowMultifactorAuthenticationTests {
    @Autowired
    @Qualifier("groovyScriptAuthenticationPolicyWebflowEventResolver")
    protected CasWebflowEventResolver resolver;

    private MockRequestContext context;

    @BeforeEach
    public void initialize() {
        this.context = new MockRequestContext();

        val request = new MockHttpServletRequest();
        request.setRemoteAddr("185.86.151.11");
        request.setLocalAddr("195.88.151.11");
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "MSIE");
        ClientInfoHolder.setClientInfo(new ClientInfo(request));

        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));

        val targetResolver = new DefaultTargetStateResolver(TestMultifactorAuthenticationProvider.ID);
        val transition = new Transition(new DefaultTransitionCriteria(
            new LiteralExpression(TestMultifactorAuthenticationProvider.ID)), targetResolver);
        context.getRootFlow().getGlobalTransitionSet().add(transition);

        WebUtils.putServiceIntoFlowScope(context, CoreAuthenticationTestUtils.getWebApplicationService());
        TestMultifactorAuthenticationProvider.registerProviderIntoApplicationContext(applicationContext);
        WebUtils.putAuthentication(CoreAuthenticationTestUtils.getAuthentication(), context);
    }

    @Test
    void verifyOperationNeedsMfa() {
        val event = resolver.resolve(context);
        assertEquals(1, event.size());
        assertEquals(TestMultifactorAuthenticationProvider.ID, event.iterator().next().getId());
    }

    @TestConfiguration(value = "GroovyMultifactorTestConfiguration", proxyBeanMethods = false)
    public static class GroovyMultifactorTestConfiguration {
        @Bean
        public MultifactorAuthenticationProvider dummyProvider() {
            return new TestMultifactorAuthenticationProvider();
        }
    }
}
