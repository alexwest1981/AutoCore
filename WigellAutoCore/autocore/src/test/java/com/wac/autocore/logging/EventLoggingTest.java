package com.wac.autocore.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.wac.autocore.OpenDays;
import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.ui.AutoCoreApp;
import com.wac.autocore.ui.i18n.I18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * E4-5: the two events the order names as examples are logged, and the reader of the window is
 * never handed the machinery.
 *
 * What the log holds is checked in {@link LogContentTest}. This class checks that the lines come
 * at all: a work order that is created and finished, a refusal, a failed database operation, and
 * the sentence the reader gets instead of the exception.
 */
class EventLoggingTest {

    private File file;

    private Db db;

    private GarageSystem garage;

    private ListAppender<ILoggingEvent> captured;

    @BeforeEach
    void openADatabase() throws Exception {
        file = File.createTempFile("eventlog-", ".db");
        file.deleteOnExit();
        file.delete();

        db = new Db(file.getAbsolutePath());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("verkstaden", Role.ADMIN));
        garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        captured = new ListAppender<ILoggingEvent>();
        captured.start();
    }

    @AfterEach
    void closeTheDatabase() {
        Logger workOrders = (Logger) LoggerFactory.getLogger(WorkOrderService.class);
        workOrders.detachAppender(captured);
        file.delete();
    }

    @Test
    void aWorkOrderThatIsCreatedAndFinishedIsLogged() {

        Logger workOrders = (Logger) LoggerFactory.getLogger(WorkOrderService.class);
        workOrders.addAppender(captured);

        WorkOrder order = aWorkOrder();

        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());

        List<ILoggingEvent> lines = captured.list;
        assertEquals(2, lines.size(), "En skapad och en slutförd order ska ge två rader");

        ILoggingEvent created = lines.get(0);
        assertEquals(Level.INFO, created.getLevel());
        assertTrue(created.getFormattedMessage().contains(String.valueOf(order.getId())),
                "Raden ska bära arbetsorderns id: " + created.getFormattedMessage());

        ILoggingEvent finished = lines.get(1);
        assertEquals(Level.INFO, finished.getLevel());
        assertTrue(finished.getFormattedMessage().contains(String.valueOf(order.getId())),
                "Raden ska bära arbetsorderns id: " + finished.getFormattedMessage());
    }

    @Test
    void aRefusalIsLoggedWithoutTheTextTheUserTyped() {

        ListAppender<ILoggingEvent> lines = attach(refusals());

        // The message may name a customer, so it is the class and the key that go in the log.
        assertThrows(RuleViolationException.class, () -> {
            throw new RuleViolationException(
                    "Kunde inte spara kund Anna Andersson på 070-1234567",
                    "dialog.workorder.no_services");
        });

        assertEquals(1, lines.list.size());

        ILoggingEvent line = lines.list.get(0);
        assertEquals(Level.WARN, line.getLevel());
        assertTrue(line.getFormattedMessage().contains("RuleViolationException"));
        assertTrue(line.getFormattedMessage().contains("dialog.workorder.no_services"));
        assertFalse(line.getFormattedMessage().contains("Anna"));
        assertFalse(line.getFormattedMessage().contains("070-1234567"));
    }

    @Test
    void aFailedDatabaseOperationIsLoggedWithItsException() {

        ListAppender<ILoggingEvent> lines = attach(refusals());

        RuntimeException cause = new RuntimeException("no such table: work_orders");
        assertThrows(DataAccessException.class, () -> {
            throw new DataAccessException("Kunde inte läsa arbetsordrarna", cause);
        });

        assertEquals(1, lines.list.size());
        ILoggingEvent line = lines.list.get(0);
        assertEquals(Level.ERROR, line.getLevel());
        assertTrue(line.getFormattedMessage().contains("Kunde inte läsa arbetsordrarna"));
        assertTrue(line.getThrowableProxy() != null, "Undantaget ska följa med för felsökning");
    }

    @Test
    void theGuardLogsARefusalOnceAndNotTwice() {

        ListAppender<ILoggingEvent> lines = attach(refusals());

        // The guard has already written who and what, so the exception stays quiet.
        assertThrows(AccessDeniedException.class, () -> {
            throw new AccessDeniedException("Role READ_ONLY may not CREATE_CUSTOMER");
        });

        assertTrue(lines.list.isEmpty(), "Nekad åtkomst ska inte loggas två gånger");
    }

    @Test
    void aRefusalReachesTheLogWithoutAnyoneInTheUiLoggingIt() {

        ListAppender<ILoggingEvent> lines = attach(refusals());

        // The way in is the console's: call the service and say something about the no. Nothing in
        // the caller writes to the log, so the line can only come from where the no was raised.
        assertThrows(AppException.class, () -> garage.createCustomer("", "", ""));
        assertThrows(AppException.class, () -> garage.createCustomer("", "", ""));

        assertEquals(2, lines.list.size(), "Två nekade försök ska ge två rader, inte noll och inte fyra");
        assertTrue(lines.list.get(0).getFormattedMessage().contains("ValidationException"));
    }

    @Test
    void theReaderGetsASentenceAndNotTheMachinery() {

        // A fault that is not ours: the class and the text are for the log.
        String shown = AutoCoreApp.errorMessageFor(
                new IllegalStateException("SQLITE_ERROR: no such table: work_orders"));

        assertFalse(shown.contains("SQLITE_ERROR"), "Intern text får inte visas: " + shown);
        assertFalse(shown.contains("IllegalStateException"), "Klassnamn får inte visas: " + shown);
        assertFalse(shown.isBlank());

        // Our own refusal is already worded, so the reader gets that sentence and nothing else.
        String refusal = AutoCoreApp.errorMessageFor(
                new AccessDeniedException("Role READ_ONLY may not CREATE_CUSTOMER"));

        assertEquals(I18n.get("dialog.access.denied"), refusal);
    }

    /** The logger every refusal writes to, whichever way the program was entered. */
    private static Logger refusals() {
        return (Logger) LoggerFactory.getLogger(AppException.class);
    }

    private static ListAppender<ILoggingEvent> attach(Logger logger) {
        ListAppender<ILoggingEvent> lines = new ListAppender<ILoggingEvent>();
        lines.start();
        logger.addAppender(lines);
        return lines;
    }

    /** A booking far enough ahead, and the work order on it. */
    private WorkOrder aWorkOrder() {

        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        Vehicle vehicle = garage.getVehicles().get(0);

        LocalDate day = OpenDays.daysAhead(40);
        Booking booking = garage.createBooking(vehicle.getId(), day, "Prov för loggen",
                LocalTime.of(8, 0), mechanic.getId(), service.getId());

        return garage.createWorkOrder(booking.getId(), mechanic.getId());
    }
}
