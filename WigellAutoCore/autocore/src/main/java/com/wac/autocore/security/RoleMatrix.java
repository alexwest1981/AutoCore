package com.wac.autocore.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Who may do what, as data. One row per role, and one place to read the rule instead of a
 * condition in every method that writes.
 *
 * The order asks for three levels, and this is how they differ:
 * an administrator may do everything, a staff member may do everything except the few things
 * below, and a read-only account may read and nothing else.
 */
public final class RoleMatrix {

    /**
     * The exceptions, written out: the only things a staff member may not do. Customers, vehicles
     * and mechanics are removed by an administrator, settings are changed by one, and an order that
     * was called off by mistake is removed by one.
     */
    private static final Set<Operation> ADMIN_ONLY = Collections.unmodifiableSet(EnumSet.of(
            Operation.DELETE_CUSTOMER,
            Operation.DELETE_VEHICLE,
            Operation.DELETE_MECHANIC,
            Operation.DELETE_WORK_ORDER,
            Operation.CHANGE_SETTINGS));

    /**
     * A read-only account reads the day's work and changes nothing, apart from choosing the language
     * for its own window. That is a preference, not a change to the workshop's data.
     *
     * The economy is not in the row. Invoices and payments say what the workshop charged and was
     * paid, and the team decided a read-only account is there to look things up, not to see that.
     */
    private static final Set<Operation> READING = Collections.unmodifiableSet(EnumSet.of(
            Operation.READ,
            Operation.CHANGE_LANGUAGE));


    /** One row per role. Built once; nothing may change it afterwards. */
    private static final Map<Role, Set<Operation>> ROWS = rows();

    private RoleMatrix() {}

    /**
     * The one question the rest of the program asks. A missing role or operation is a no: a guard
     * that cannot tell what it is looking at must not let anyone through.
     */
    public static boolean allows(Role role, Operation operation) {
        if (role == null || operation == null) {
            return false;
        }
        Set<Operation> row = ROWS.get(role);
        return row != null && row.contains(operation);
    }

    /**
     * The matrix as text, one line per role. It is what the proof prints, and it lets the rule be
     * read without reading the code.
     */
    public static String describe() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Role, Set<Operation>> row : ROWS.entrySet()) {
            text.append(String.format("%-9s ", row.getKey()));
            Set<Operation> allowed = row.getValue();
            if (allowed.size() == Operation.values().length) {
                text.append("allt (").append(allowed.size()).append(" operationer)");
            } else if (allowed.equals(READING)) {
                text.append("läsa dagens arbete och välja språk, inte ekonomin");
            } else {
                EnumSet<Operation> refused = EnumSet.allOf(Operation.class);
                refused.removeAll(allowed);
                text.append("allt utom ").append(refused);
            }
            text.append(System.lineSeparator());
        }
        return text.toString();
    }

    /**
     * Builds the rows. The administrator's row is every operation there is, by definition: a new
     * operation is something only an administrator may do until someone says otherwise, and the
     * exceptions are then taken away from the staff member.
     */
    private static Map<Role, Set<Operation>> rows() {
        Map<Role, Set<Operation>> rows = new EnumMap<Role, Set<Operation>>(Role.class);
        rows.put(Role.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(Operation.class)));

        EnumSet<Operation> staff = EnumSet.allOf(Operation.class);
        staff.removeAll(ADMIN_ONLY);
        rows.put(Role.STAFF, Collections.unmodifiableSet(staff));

        rows.put(Role.READ_ONLY, READING);

        return Collections.unmodifiableMap(rows);
    }
}
