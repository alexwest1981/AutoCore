package com.wac.autocore.service;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.seed.SeedText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Who may perform which service. The service requires a specialization, the mechanic carries their own. */
public class MechanicRules {

    /** The seeded generalist, written as a code so no translated word has to be recognized. */
    private static final String GENERALIST_CODE = "mechanic.general_service";

    private final MechanicRepository mechanicRepository;

    public MechanicRules(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public List<Mechanic> getAll() {
        try {
            return mechanicRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read mechanics", e);
        }
    }

    public List<Mechanic> qualifiedFor(ServiceItem service) {
        if (service == null) {
            return getAll();
        }
        return qualifiedFor(Collections.singletonList(service));
    }

    public List<Mechanic> qualifiedFor(Collection<ServiceItem> services) {
        List<Mechanic> all = getAll();
        if (services == null || services.isEmpty()) {
            return all;
        }
        List<Mechanic> qualified = new ArrayList<Mechanic>();
        for (Mechanic m : all) {
            boolean allQualified = true;
            for (ServiceItem s : services) {
                if (!isMechanicQualified(m, s)) {
                    allQualified = false;
                    break;
                }
            }
            if (allQualified) {
                qualified.add(m);
            }
        }
        if (qualified.isEmpty() && services.size() == 1) {
            for (Mechanic m : all) {
                if (isGeneralist(m)) {
                    qualified.add(m);
                }
            }
        }
        qualified.sort(new Comparator<Mechanic>() {
            @Override
            public int compare(Mechanic m1, Mechanic m2) {
                boolean g1 = isGeneralist(m1);
                boolean g2 = isGeneralist(m2);
                // A service nobody has to be qualified for goes to the generalist first. The rule
                // reads the requirement the service carries, not the words in its name.
                boolean anyMechanicWillDo = services.stream().allMatch(s -> s.requiresAnyMechanic());
                if (anyMechanicWillDo) {
                    if (g1 && !g2) return -1;
                    if (!g1 && g2) return 1;
                }
                return m1.getName().compareToIgnoreCase(m2.getName());
            }
        });
        return qualified;
    }

    /** The mechanics needed to staff the services. */
    public List<Mechanic> requiredFor(Collection<ServiceItem> services) {
        List<Mechanic> result = new ArrayList<Mechanic>();
        if (services == null || services.isEmpty()) {
            return result;
        }
        List<Mechanic> all = getAll();
        for (ServiceItem s : services) {
            Mechanic best = null;
            // A service with no requirement should go to the generalist, even if a specialist is already picked.
            boolean anyQualified = s.requiresAnyMechanic();
            for (Mechanic m : result) {
                if (isMechanicQualified(m, s) && (!anyQualified || isGeneralist(m))) {
                    best = m;
                    break;
                }
            }
            if (best == null && anyQualified) {
                for (Mechanic m : all) {
                    if (isGeneralist(m)) {
                        best = m;
                        break;
                    }
                }
            }
            if (best == null) {
                for (Mechanic m : all) {
                    if (isMechanicQualified(m, s)) {
                        best = m;
                        break;
                    }
                }
            }
            if (best == null) {
                for (Mechanic m : all) {
                    if (isGeneralist(m)) {
                        best = m;
                        break;
                    }
                }
            }
            // If no qualified mechanic is found the service is left out of the team. Padding with
            // a mechanic who lacks the qualification gives a picked field the check then rejects.
            if (best != null && !result.contains(best)) {
                result.add(best);
            }
        }
        return result;
    }

    /** How long the car holds the shop: the mechanic who gets the most work decides.
     *  Adding up every service instead gives the car a window where it was done long ago. */
    public int busyMinutes(Collection<ServiceItem> services, List<Mechanic> team) {
        if (services == null || services.isEmpty()) {
            return 60;
        }

        Map<Integer, Integer> perMechanic = new LinkedHashMap<Integer, Integer>();
        for (ServiceItem service : services) {
            if (service == null) {
                continue;
            }
            Mechanic who = firstQualifiedInTeam(team, service);
            if (who == null) {
                continue;
            }
            Integer done = perMechanic.get(Integer.valueOf(who.getId()));
            perMechanic.put(Integer.valueOf(who.getId()), Integer.valueOf(
                    (done == null ? 0 : done.intValue()) + service.getEstimatedMinutes()));
        }

        int busiest = 0;
        for (Integer minutes : perMechanic.values()) {
            busiest = Math.max(busiest, minutes.intValue());
        }
        if (busiest > 0) {
            return busiest;
        }

        // No mechanic could be pointed out: the sum is all we know about the time.
        int total = 0;
        for (ServiceItem service : services) {
            if (service != null) {
                total += service.getEstimatedMinutes();
            }
        }
        return total > 0 ? total : 60;
    }

        /** The first mechanic in the team who may perform the service. The same pick the work orders make. */
    private Mechanic firstQualifiedInTeam(List<Mechanic> team, ServiceItem service) {
        if (team == null) {
            return null;
        }
        for (Mechanic mechanic : team) {
            if (isMechanicQualified(mechanic, service)) {
                return mechanic;
            }
        }
        return null;
    }

        /** True if the mechanic is a generalist and can take services with no requirement. */
    public boolean isGeneralist(Mechanic mechanic) {
        if (mechanic == null) {
            return false;
        }
        String stored = mechanic.getSpecialization();
        if (GENERALIST_CODE.equals(SeedText.code(stored))) {
            return true;
        }
        // A specialization typed by hand carries no code. Only those keep the old lenient read,
        // so a seeded mechanic is never recognized by a word in the current language.
        return !SeedText.isKey(stored) && mentionsGeneral(stored);
    }

    private static boolean mentionsGeneral(String stored) {
        String text = stored == null ? "" : stored.trim().toLowerCase(java.util.Locale.ROOT);
        return text.contains("general") || text.contains("allmän");
    }

    public boolean isMechanicQualified(Mechanic mechanic, ServiceItem service) {
        if (service == null) {
            return true;
        }
        if (mechanic == null) {
            return false;
        }
        if (service.requiresAnyMechanic()) {
            return true;
        }
        String needed = service.getSpecialization().trim();
        String has = mechanic.getSpecialization();
        return has != null && needed.equals(has.trim());
    }
}
