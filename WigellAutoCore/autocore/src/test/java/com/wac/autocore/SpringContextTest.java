package com.wac.autocore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.service.PasswordRotation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * SCRUM-328: the platform the order asks for in §2, held in place by a test instead of by whoever
 * remembers to start the window.
 *
 * The path is the part worth pinning. E2-1 made the storage a bean built from
 * {@code autocore.db.path}, so the property and the context have a contract. If the property is
 * ignored, the app quietly writes to {@code data/autocore.db} instead of the file the run asked
 * for, and nothing says so until someone looks for their data in the wrong place.
 */
@SpringBootTest
class SpringContextTest {

    /** A file nobody has created yet, so its existence is the answer to "did the property arrive". */
    private static final Path DATABASE =
            Paths.get(System.getProperty("java.io.tmpdir"), "autocore-context-" + UUID.randomUUID() + ".db");

    @DynamicPropertySource
    static void databasePath(DynamicPropertyRegistry registry) {
        registry.add("autocore.db.path", DATABASE::toString);
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Db db;

    @Autowired
    private RepositoryFactory repositories;

    @Autowired
    private IdentityHolder identity;

    @AfterAll
    static void removeTheDatabase() throws IOException {
        Files.deleteIfExists(DATABASE);
    }

    @Test
    void theContextStartsWithTheDatabaseFromTheConfiguration() throws IOException {

        // Building the beans does not create the file; the schema is written when something asks
        // for it. So this test can check the whole round trip: property in, file out.
        Files.deleteIfExists(DATABASE);
        assertFalse(Files.exists(DATABASE), "kontexten ska inte skriva databasen innan någon ber om den");

        db.ensureReady();

        assertTrue(Files.exists(DATABASE),
                "schemat ska hamna i " + DATABASE + ", inte i " + Db.DEFAULT_PATH
                        + ". Gör det inte det har autocore.db.path inte nått bönan.");
    }

    @Test
    void theBeansAreTheOnesTheConfigurationBuilds() {

        assertSame(db, repositories.db(), "repository-fabriken ska läsa samma fil som bönan");
        assertTrue(identity.current().isEmpty(), "ingen ska vara inloggad när kontexten startar");

        assertNotNull(context.getBean(LoginService.class));
        assertNotNull(context.getBean(PasswordRotation.class));
    }
}
