package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationHandlersConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPolicyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreLogoutConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreTicketsSerializationConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasMongoAuthenticationConfiguration;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.cas.config.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.mongo.MongoDbConnectionFactory;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;

import lombok.val;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for {@link MongoDbAuthenticationHandler}.
 *
 * @author Misagh Moayyed
 * @since 4.2
 */
@SpringBootTest(classes = {
    CasMongoAuthenticationConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreServicesAuthenticationConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreAuthenticationPolicyConfiguration.class,
    CasCoreAuthenticationMetadataConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class,
    CasCoreAuthenticationHandlersConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    CasCoreTicketIdGeneratorsConfiguration.class,
    CasCoreTicketsConfiguration.class,
    CasCoreTicketsSerializationConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class,
    CasPersonDirectoryConfiguration.class,
    CasCoreWebConfiguration.class,
    CasCoreLogoutConfiguration.class,
    CasCoreConfiguration.class,
    RefreshAutoConfiguration.class
}, properties = {
    "cas.authn.mongo.client-uri=mongodb://root:secret@localhost:27017/admin",
    "cas.authn.mongo.collection=users",
    "cas.authn.mongo.database-name=cas",
    "cas.authn.mongo.attributes=loc,state",
    "cas.authn.mongo.username-attribute=username",
    "cas.authn.mongo.password-attribute=password"
})
@EnableScheduling
@EnabledIfListeningOnPort(port = 27017)
@Tag("MongoDb")
@EnableConfigurationProperties(CasConfigurationProperties.class)
class MongoDbAuthenticationHandlerTests {
    @Autowired
    @Qualifier("mongoAuthenticationHandler")
    private AuthenticationHandler authenticationHandler;

    @Autowired
    private CasConfigurationProperties casProperties;

    @BeforeEach
    public void initialize() {
        val mongo = casProperties.getAuthn().getMongo();
        val factory = new MongoDbConnectionFactory();
        try (val mongoClient = factory.buildMongoDbClient(mongo)) {
            val database = mongoClient.getDatabase(mongo.getDatabaseName());
            database.drop();
            val col = database.getCollection(mongo.getCollection());

            var account = new Document();
            account.append(mongo.getUsernameAttribute(), "u1");
            account.append(mongo.getPasswordAttribute(), "p1");
            account.append("loc", "Apereo");
            account.append("state", "California");
            col.insertOne(account);

            account = new Document();
            account.append(mongo.getUsernameAttribute(), "userPlain");
            col.insertOne(account);
        }
    }

    @Test
    void verifyAuthentication() throws Exception {
        val creds = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("u1", "p1");
        val result = authenticationHandler.authenticate(creds, mock(Service.class));
        assertEquals("u1", result.getPrincipal().getId());
        val attributes = result.getPrincipal().getAttributes();
        assertTrue(attributes.containsKey("loc"));
        assertTrue(attributes.containsKey("state"));
    }

    @Test
    void verifyAuthenticationFails() {
        val creds = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("unknown", "p1");
        assertThrows(AccountNotFoundException.class, () -> authenticationHandler.authenticate(creds, mock(Service.class)));
    }

    @Test
    void verifyNoPsw() {
        val creds = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("userPlain", "p1");
        assertThrows(FailedLoginException.class, () -> authenticationHandler.authenticate(creds, mock(Service.class)));
    }

    @Test
    void verifyBadPsw() {
        val creds = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("u1", "other");
        assertThrows(FailedLoginException.class, () -> authenticationHandler.authenticate(creds, mock(Service.class)));
    }
}
