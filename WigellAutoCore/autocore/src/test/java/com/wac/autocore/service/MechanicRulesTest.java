package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.util.Texts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * The mechanics a service is matched with. The rules read the stored codes, so the language the
 * window happens to speak cannot change who gets the job.
 */
class MechanicRulesTest {

    private File file;

    private MechanicRules rules;

    private com.wac.autocore.repository.MechanicRepository mechanics;

    private List<ServiceItem> services;

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("mechanic-rules-", ".db");
            file.deleteOnExit();
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();

            SqliteRepositoryFactory repositories = new SqliteRepositoryFactory(db);
            mechanics = repositories.createMechanicRepository();
            rules = new MechanicRules(mechanics);
            services = repositories.createServiceItemRepository().findAll();

        } catch (Exception cannotStart) {
            throw new IllegalStateException("Kunde inte bygga provets databas", cannotStart);
        }
    }

    @AfterEach
    void removeDatabase() {
        Texts.use(Texts.DEFAULT_LANG);
        if (file != null) {
            file.delete();
        }
    }

    /** The seeded generalist, whoever speaks which language. */
    @Test
    void theSeededGeneralistIsFoundInBothLanguages() {
        Texts.use(Texts.LANG_SV);
        boolean inSwedish = rules.getAll().stream().anyMatch(rules::isGeneralist);

        Texts.use(Texts.LANG_EN);
        boolean inEnglish = rules.getAll().stream().anyMatch(rules::isGeneralist);

        assertTrue(inSwedish);
        assertEquals(inSwedish, inEnglish);
    }

    /** A service that needs brakes goes to the mechanic who has brakes, in either language. */
    @Test
    void aSpecialistServicePicksTheSameMechanicInBothLanguages() {
        ServiceItem brakes = serviceNamed("brake_service");
        assertFalse(brakes.requiresAnyMechanic());

        Texts.use(Texts.LANG_SV);
        List<Mechanic> inSwedish = rules.qualifiedFor(brakes);

        Texts.use(Texts.LANG_EN);
        List<Mechanic> inEnglish = rules.qualifiedFor(brakes);

        assertFalse(inSwedish.isEmpty());
        assertEquals(names(inSwedish), names(inEnglish));
        assertTrue(inSwedish.stream().allMatch(m -> rules.isMechanicQualified(m, brakes)));
    }

    /** A service anyone can do puts the generalist first, without reading the service's name. */
    @Test
    void aServiceForAnyoneStillStartsWithTheGeneralist() {
        ServiceItem oil = serviceNamed("oil_change");
        assertTrue(oil.requiresAnyMechanic());

        Texts.use(Texts.LANG_EN);
        List<Mechanic> qualified = rules.qualifiedFor(oil);

        assertFalse(qualified.isEmpty());
        assertTrue(rules.isGeneralist(qualified.get(0)));
    }

    /** Text typed by hand carries no code, so the old lenient read stays for those. */
    @Test
    void aHandTypedGeneralistIsStillAGeneralist() {
        Mechanic typed = new Mechanic(0, "Prov", "070-0000000", "Allmän");
        assertTrue(rules.isGeneralist(typed));

        Mechanic specialist = new Mechanic(0, "Prov", "070-0000000", "Bromsar");
        assertFalse(rules.isGeneralist(specialist));
    }

    private ServiceItem serviceNamed(String code) {
        for (ServiceItem service : services) {
            String name = service.getName() == null ? "" : service.getName();
            if (name.contains(code)) {
                return service;
            }
        }
        throw new IllegalStateException("Hittade ingen tjänst med " + code + " i namnet");
    }

    private List<String> names(List<Mechanic> mechanics) {
        return mechanics.stream().map(Mechanic::getName).sorted().toList();
    }

    /**
     * The rule reads what the service requires, not the words in its name. A mechanic who sorts
     * first by name must not push the generalist out of the first place.
     */
    @Test
    void aServiceForAnyoneGoesToTheGeneralistEvenWhenAnotherNameSortsFirst() {
        mechanics.save(new Mechanic(0, "Aaron Andersson", "070-1111111", "seed.mechanic.brakes.specialization"));

        ServiceItem contactCheck = new ServiceItem(0, "Bromskontroll", "", 0, 30, "");

        Texts.use(Texts.LANG_EN);
        List<Mechanic> qualified = rules.qualifiedFor(contactCheck);

        assertEquals("Johan Karlsson", qualified.get(0).getName());
        assertTrue(rules.isGeneralist(qualified.get(0)));
    }
}
