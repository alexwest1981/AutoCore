package com.wac.autocore.test;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.i18n.I18n;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prov för felrapporten: fyra tjänster i en bokning valde tre mekaniker automatiskt, och sedan gick
 * bokningen inte att skapa med beskedet att en mekaniker saknade behörighet för en tjänst.
 *
 * Orsaken var tjänsten "Oljebyte". Behörigheten letade efter "olja" i tjänstens namn, och "Oljebyte"
 * innehåller inte det ordet. På engelska heter tjänsten "Oil change" och innehåller "oil", så felet
 * syntes bara när gränssnittet körde på svenska. Beskrivningen, som säger "motorolja", lästes inte.
 */
public class MechanicQualificationTest {

    /** Varje demotjänst ska ha minst en behörig mekaniker, oavsett språk. */
    public void testEverySeededServiceHasAQualifiedMechanicInBothLanguages() throws Exception {
        String original = I18n.getLanguage();
        try {
            for (String language : new String[]{"sv", "en"}) {
                I18n.setLanguage(language);
                GarageSystem garage = new GarageSystem();
                for (ServiceItem service : garage.getServiceItems()) {
                    boolean found = false;
                    for (Mechanic mechanic : garage.getMechanics()) {
                        if (garage.isMechanicQualified(mechanic, service)) {
                            found = true;
                            break;
                        }
                    }
                    TestRunner.assertTrue(found, "Ingen mekaniker är behörig för "
                            + SeedText.resolve(service.getName()) + " när gränssnittet kör på " + language);
                }
            }
        } finally {
            I18n.setLanguage(original);
        }
    }

    /**
     * Varje tjänst i bokningen måste täckas av teamet. Är den inte det säger fältet att teamet är
     * klart medan kontrollen säger nej, och bokningen går inte att skapa.
     */
    public void testTheChosenTeamCoversEveryServiceInTheBooking() throws Exception {
        String original = I18n.getLanguage();
        try {
            for (String language : new String[]{"sv", "en"}) {
                I18n.setLanguage(language);
                GarageSystem garage = new GarageSystem();
                List<ServiceItem> services = garage.getServiceItems();
                List<Mechanic> team = garage.getRequiredMechanics(services);

                for (ServiceItem service : services) {
                    boolean covered = false;
                    for (Mechanic mechanic : team) {
                        if (garage.isMechanicQualified(mechanic, service)) {
                            covered = true;
                            break;
                        }
                    }
                    TestRunner.assertTrue(covered, "Teamet saknar behörighet för "
                            + SeedText.resolve(service.getName()) + " när gränssnittet kör på " + language);
                }
            }
        } finally {
            I18n.setLanguage(original);
        }
    }

    /** Ingen mekaniker ska hamna i teamet utan att vara behörig för minst en av bokningens tjänster. */
    public void testTheTeamNeverContainsAMechanicWithoutAQualification() throws Exception {
        String original = I18n.getLanguage();
        try {
            I18n.setLanguage("sv");
            GarageSystem garage = new GarageSystem();
            List<ServiceItem> services = garage.getServiceItems();
            List<Mechanic> team = garage.getRequiredMechanics(services);

            Set<Integer> ids = new HashSet<Integer>();
            for (Mechanic mechanic : team) {
                ids.add(Integer.valueOf(mechanic.getId()));
                boolean qualified = false;
                for (ServiceItem service : services) {
                    if (garage.isMechanicQualified(mechanic, service)) {
                        qualified = true;
                        break;
                    }
                }
                TestRunner.assertTrue(qualified, mechanic.getName()
                        + " står i teamet men saknar behörighet för varje tjänst i bokningen");
            }
            TestRunner.assertEquals(team.size(), ids.size(),
                    "Samma mekaniker får inte stå två gånger i teamet, teamet var: " + ids);
        } finally {
            I18n.setLanguage(original);
        }
    }

    /** En tjänst utan krav får utföras av alla, och en tjänst med krav bara av sin specialist. */
    public void testARequirementOnTheServiceControlsWhoMayTakeIt() throws Exception {
        String original = I18n.getLanguage();
        try {
            I18n.setLanguage("sv");
            GarageSystem garage = new GarageSystem();

            Mechanic brakeSpecialist = new Mechanic(1, "Bromsspecialist", "070-1",
                    "seed.mechanic.brakes.specialization");
            Mechanic generalist = new Mechanic(2, "Allmän", "070-2",
                    "seed.mechanic.general_service.specialization");

            ServiceItem open = new ServiceItem(1, "Oljebyte", "Byte av motorolja", 1295, 45);
            ServiceItem brakes = new ServiceItem(2, "Bromsservice", "Bromsbelägg", 2495, 90,
                    "seed.mechanic.brakes.specialization");

            TestRunner.assertTrue(garage.isMechanicQualified(generalist, open),
                    "En tjänst utan krav ska gå att lägga på vilken mekaniker som helst");
            TestRunner.assertTrue(garage.isMechanicQualified(brakeSpecialist, open),
                    "En tjänst utan krav ska även gå till en specialist");
            TestRunner.assertTrue(garage.isMechanicQualified(brakeSpecialist, brakes),
                    "Bromsspecialisten ska klara bromsservicen");
            TestRunner.assertFalse(garage.isMechanicQualified(generalist, brakes),
                    "Allmänmekanikern ska inte klara en tjänst som kräver bromsspecialisten");
        } finally {
            I18n.setLanguage(original);
        }
    }
}
