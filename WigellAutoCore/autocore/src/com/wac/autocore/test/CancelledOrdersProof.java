package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.navigation.PageRouter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The work order list keeps the cancelled orders out, and the road back to one still works.
 *
 * The page itself needs a window, but the list it is built from does not: what is checked here is
 * the rows the page gets and the switch that widens them. The jump from the schedule goes through
 * the same two, and is the reason the switch exists at all — a cancelled order can be started
 * again from its details dialog, and that road goes through a row in this list.
 */
public final class CancelledOrdersProof {

    private CancelledOrdersProof() {}

    public static void main(String[] args) throws Exception {

        File database = File.createTempFile("cancelled-orders-", ".db");
        database.deleteOnExit();
        // Nothing to copy: the program makes the database itself.
        database.delete();

        Db.use(database.getAbsolutePath());
        Db.ensureReady();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory());
        PageRouter router = new PageRouter(garage, null);

        // One order is cancelled the way the button does it, and one is left open beside it.
        int vehicleId = garage.getVehicles().get(0).getId();
        WorkOrder cancelled = garage.createDraft(vehicleId, "Utkast som avbryts");
        garage.cancelWorkOrder(cancelled.getId());
        // The object above was written before the change, so the status is read back from the row.
        cancelled = garage.findWorkOrder(cancelled.getId());
        int openId = openOrderBeside(garage, cancelled.getId());

        int all = garage.getWorkOrders().size();

        List<WorkOrder> shown = router.workOrdersForList();
        boolean out = shown.size() == all - 1 && !holds(shown, cancelled.getId());

        boolean recognised = router.isCancelledWorkOrder(cancelled.getId())
                && !router.isCancelledWorkOrder(openId);

        router.setShowCancelledWorkOrders(true);
        List<WorkOrder> widened = router.workOrdersForList();
        boolean in = widened.size() == all && holds(widened, cancelled.getId());

        router.setShowCancelledWorkOrders(false);
        boolean outAgain = router.workOrdersForList().size() == all - 1;

        System.out.println("Avbrutna arbetsordrar i listan");
        System.out.println();
        System.out.println("arbetsordrar totalt: " + all + ", varav en avbruten (nummer "
                + cancelled.getId() + ", " + cancelled.getStatus() + ")");
        System.out.println();
        System.out.println("listan lämnar den utanför:               " + out);
        System.out.println("den avbrutna känns igen, den öppna inte: " + recognised);
        System.out.println("kryssrutan tar in den:                   " + in);
        System.out.println("av igen lämnar den utanför:              " + outAgain);
        System.out.println();
        System.out.println("listan:         " + ids(shown));
        System.out.println("med avbrutna:   " + ids(widened));
        System.out.println();
        System.out.println("sidan själv behöver ett fönster. Det som prövas är raderna den byggs av.");
        System.exit(out && recognised && in && outAgain ? 0 : 1);
    }

    /** The id of a work order that is not cancelled, so the switch has something to leave alone. */
    private static int openOrderBeside(GarageSystem garage, int cancelledId) {
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getId() != cancelledId) {
                return order.getId();
            }
        }
        throw new IllegalStateException("Ingen annan arbetsorder fanns att jämföra med.");
    }

    private static boolean holds(List<WorkOrder> orders, int id) {
        for (WorkOrder order : orders) {
            if (order.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> ids(List<WorkOrder> orders) {
        List<Integer> out = new ArrayList<Integer>();
        for (WorkOrder order : orders) {
            out.add(Integer.valueOf(order.getId()));
        }
        return out;
    }
}
