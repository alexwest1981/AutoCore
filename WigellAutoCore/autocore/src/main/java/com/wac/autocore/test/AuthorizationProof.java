package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.NotAuthenticatedException;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;
import com.wac.autocore.service.LoginService;

import java.io.File;
import java.util.Optional;

/**
 * E3-5 and E3-7: the same operations, tried by nobody, by each of the three accounts, and printed as
 * a table. Reading is one of the operations, because a caller with no identity must not reach the
 * data any more than it may change it.
 *
 * The accounts and their passwords are given on the command line, so no password is written into
 * the repository:
 *
 * <pre>
 *   AuthorizationProof admin:... staff:... readonly:...
 * </pre>
 *
 * Exit 0 when every answer is the one the order asks for, 1 otherwise.
 */
public final class AuthorizationProof {

    private AuthorizationProof() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Användning: AuthorizationProof användare:lösenord ...");
            System.exit(2);
        }

        File file;
        Db db;
        try {
            file = File.createTempFile("authorization-proof-", ".db");
            file.deleteOnExit();
            file.delete();
            db = new Db(file.getAbsolutePath());
            db.ensureReady();
        } catch (Exception cannotStart) {
            System.out.println("Provet kunde inte bygga sin databas: " + cannotStart);
            System.exit(2);
            return;
        }

        System.out.println("E3-5 · samma åtgärd, olika svar per roll");
        System.out.println("register: " + file.getAbsolutePath() + " (byggt av provet)");
        System.out.println();

        IdentityHolder holder = new IdentityHolder();
        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), holder);
        LoginService login = new LoginService(new UserRepository(db));

        boolean holds = true;

        // 1. Nobody signed in: a different answer from "signed in, but not allowed".
        holder.clear();
        String nobody = create(garage, 1);
        holds &= nobody.startsWith("nekad (inte inloggad)");

        System.out.printf("%-16s %-30s %-30s %s%n", "roll", "getCustomers", "createCustomer", "saveSetting");
        System.out.printf("%-16s %-30s %-30s %s%n", "ingen inloggad", read(garage), nobody, "-");
        String[] roles = new String[args.length];
        String[] creates = new String[args.length];
        String[] settings = new String[args.length];
        String[] reads = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            String[] parts = args[i].split(":", 2);
            Optional<Identity> identity = login.signIn(parts[0], parts.length > 1 ? parts[1] : "");
            if (identity.isEmpty()) {
                System.out.println(args[i].split(":")[0] + ": kunde inte logga in");
                System.exit(1);
                return;
            }
            holder.set(identity.get());
            roles[i] = identity.get().role().toString();
            reads[i] = read(garage);
            creates[i] = create(garage, i + 2);
            settings[i] = changeSetting(garage);
            System.out.printf("%-16s %-30s %-30s %s%n", roles[i], reads[i], creates[i], settings[i]);
            holds &= isExpected(identity.get().role(), reads[i], creates[i], settings[i]);
        }

        System.out.println();
        System.out.println("stämmer med kraven: " + holds);
        System.exit(holds ? 0 : 1);
    }

    /**
     * The answer each level is supposed to get: everybody signed in may read, the administrator does
     * everything, a staff member may create a customer but not change the settings, and a read-only
     * account may do neither.
     */
    private static boolean isExpected(Role role, String read, String create, String setting) {
        if (!read.startsWith("tillåts")) {
            return false;
        }
        if (role == Role.ADMIN) {
            return "tillåts".equals(create) && "tillåts".equals(setting);
        }
        if (role == Role.STAFF) {
            return "tillåts".equals(create) && setting.startsWith("nekad (AccessDenied");
        }
        return create.startsWith("nekad (AccessDenied") && setting.startsWith("nekad (AccessDenied");
    }

    /** Reads the customer list and names the answer, so nobody can slip past the window unseen. */
    private static String read(GarageSystem garage) {
        try {
            return "tillåts (" + garage.getCustomers().size() + " kunder)";
        } catch (NotAuthenticatedException notSignedIn) {
            return "nekad (inte inloggad)";
        } catch (RuntimeException refused) {
            return "nekad (" + refused.getClass().getSimpleName() + ")";
        }
    }

    /**
     * Creates one customer and names the answer. The name has no digits in it and the number is
     * unique per call, because the model refuses both of those for other reasons than the guard.
     */
    private static String create(GarageSystem garage, int serial) {
        String phone = "070-" + String.format("%07d", 1000000 + serial);
        try {
            garage.createCustomer("Provkund", phone, "prov@exempel.com");
            return "tillåts";
        } catch (NotAuthenticatedException notSignedIn) {
            return "nekad (inte inloggad)";
        } catch (RuntimeException refused) {
            return "nekad (" + refused.getClass().getSimpleName() + ")";
        }
    }

    private static String changeSetting(GarageSystem garage) {
        try {
            garage.saveSetting("probe", "1");
            return "tillåts";
        } catch (NotAuthenticatedException notSignedIn) {
            return "nekad (inte inloggad)";
        } catch (RuntimeException refused) {
            return "nekad (" + refused.getClass().getSimpleName() + ")";
        }
    }
}
