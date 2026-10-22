package com.wac.autocore.proof;

import com.wac.autocore.security.Operation;
import com.wac.autocore.security.Role;
import com.wac.autocore.security.RoleMatrix;

/**
 * E3-3: the rule about who may do what is data and can be read, not a condition buried in each
 * method. The proof prints the matrix and asks it the question the order asks: a read-only account
 * may not take a work order out of the flow, an administrator may.
 *
 * No arguments and no database: the matrix is code, the same on every machine.
 */
public final class RoleMatrixProof {

    private RoleMatrixProof() {}

    public static void main(String[] args) {
        System.out.println("E3-3 · rollmatrisen som data");
        System.out.println();
        System.out.print(RoleMatrix.describe());
        System.out.println();

        say(Role.ADMIN, Operation.CANCEL_WORK_ORDER);
        say(Role.STAFF, Operation.CANCEL_WORK_ORDER);
        say(Role.READ_ONLY, Operation.CANCEL_WORK_ORDER);
        System.out.println();

        say(Role.STAFF, Operation.DELETE_CUSTOMER);
        say(Role.STAFF, Operation.CHANGE_SETTINGS);
        say(Role.READ_ONLY, Operation.READ);

        boolean asTheOrderSays =
                RoleMatrix.allows(Role.ADMIN, Operation.CANCEL_WORK_ORDER)
                && !RoleMatrix.allows(Role.READ_ONLY, Operation.CANCEL_WORK_ORDER)
                && !RoleMatrix.allows(Role.STAFF, Operation.DELETE_CUSTOMER)
                && RoleMatrix.allows(Role.READ_ONLY, Operation.READ);

        System.out.println();
        System.out.println("stämmer med kraven: " + asTheOrderSays);
        System.exit(asTheOrderSays ? 0 : 1);
    }

    private static void say(Role role, Operation operation) {
        System.out.println(String.format("%-9s %-20s %s",
                role, operation, RoleMatrix.allows(role, operation) ? "tillåts" : "nekas"));
    }
}
