package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.seed.SeedText;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vem som får utföra vilken tjänst. Tjänsten kräver en specialisering, mekanikern bär sin. */
public class MechanicRules {

    private final MechanicRepository mechanicRepository = new MechanicRepository();

    public List<Mechanic> getAll() {
        try {
            return mechanicRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read mechanics: " + e.getMessage());
            return Collections.emptyList();
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
                String resolvedSpec = SeedText.resolve(m.getSpecialization());
                String s = resolvedSpec != null ? resolvedSpec.toLowerCase() : "";
                if (s.contains("general") || s.contains("allmän")) {
                    qualified.add(m);
                }
            }
        }
        qualified.sort(new Comparator<Mechanic>() {
            @Override
            public int compare(Mechanic m1, Mechanic m2) {
                String spec1 = SeedText.resolve(m1.getSpecialization()).toLowerCase();
                String spec2 = SeedText.resolve(m2.getSpecialization()).toLowerCase();
                boolean g1 = spec1.contains("general") || spec1.contains("allmän");
                boolean g2 = spec2.contains("general") || spec2.contains("allmän");
                boolean onlyGeneral = services.stream().allMatch(s -> {
                    String n = SeedText.resolve(s.getName()).toLowerCase();
                    return n.contains("oil") || n.contains("olja") || n.contains("annual") || n.contains("årlig");
                });
                if (onlyGeneral) {
                    if (g1 && !g2) return -1;
                    if (!g1 && g2) return 1;
                }
                return m1.getName().compareToIgnoreCase(m2.getName());
            }
        });
        return qualified;
    }

/** Mekanikerna som behövs för att bemanna tjänsterna. */
    public List<Mechanic> requiredFor(Collection<ServiceItem> services) {
        List<Mechanic> result = new ArrayList<Mechanic>();
        if (services == null || services.isEmpty()) {
            return result;
        }
        List<Mechanic> all = getAll();
        for (ServiceItem s : services) {
            Mechanic best = null;
            // En tjänst utan krav ska den generella mekanikern ta, även om en specialist redan är vald.
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
            // Hittas ingen behörig mekaniker lämnas tjänsten utanför teamet. Att fylla på med en
            // mekaniker som saknar behörigheten ger ett valt fält som kontrollen sedan underkänner.
            if (best != null && !result.contains(best)) {
                result.add(best);
            }
        }
        return result;
    }

    /** Hur länge bilen håller verkstaden: den mekaniker som får mest arbete bestämmer.
     *  Summerar man i stället alla tjänster får bilen ett fönster där den för länge sedan är klar. */
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

        // Ingen mekaniker kunde pekas ut: summan är det enda vi vet om tiden.
        int total = 0;
        for (ServiceItem service : services) {
            if (service != null) {
                total += service.getEstimatedMinutes();
            }
        }
        return total > 0 ? total : 60;
    }

    /** Första mekanikern i teamet som får utföra tjänsten. Samma val som arbetsordrarna gör. */
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

    /** Sant om mekanikern är generalist och kan ta tjänster utan krav. */
    public boolean isGeneralist(Mechanic mechanic) {
        if (mechanic == null) {
            return false;
        }
        String spec = SeedText.resolve(mechanic.getSpecialization());
        String specStr = spec != null ? spec.toLowerCase() : "";
        return specStr.contains("general") || specStr.contains("allmän");
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
