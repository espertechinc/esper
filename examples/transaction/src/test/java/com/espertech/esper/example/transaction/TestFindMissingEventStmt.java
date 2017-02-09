package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;

public class TestFindMissingEventStmt extends TestStmtBase {
    private static final int TIME_WINDOW_SIZE_MSEC = 1800 * 1000;

    private SupportUpdateListener listener;

    public void setUp() {
        super.setUp();

        listener = new SupportUpdateListener();
        FindMissingEventStmt stmt = new FindMissingEventStmt(epService.getEPAdministrator());
        stmt.addListener(listener);

        // Use external clocking for the test
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    }

    public void testFlow() {
        TxnEventA a[] = new TxnEventA[20];
        TxnEventB b[] = new TxnEventB[20];
        TxnEventC c[] = new TxnEventC[20];


        int seconds = 1 * 60;   // after 1 minutes
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(a[0] = new TxnEventA("id0", seconds * 1000, "c1"));
        sendEvent(b[0] = new TxnEventB("id0", seconds * 1000));
        sendEvent(c[0] = new TxnEventC("id0", seconds * 1000, "s1"));

        seconds = 2 * 60;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(a[1] = new TxnEventA("id1", seconds * 1000, "c1"));

        seconds = 3 * 60;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(b[2] = new TxnEventB("id2", seconds * 1000));

        seconds = 4 * 60;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(b[3] = new TxnEventB("id3", seconds * 1000));
        sendEvent(c[3] = new TxnEventC("id3", seconds * 1000, "s1"));

        seconds = 5 * 60;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(a[4] = new TxnEventA("id4", seconds * 1000, "c1"));
        sendEvent(c[4] = new TxnEventC("id4", seconds * 1000, "s1"));

        seconds = 6 * 60;
        sendEvent(new CurrentTimeEvent(seconds * 1000));
        sendEvent(a[5] = new TxnEventA("id5", seconds * 1000, "c1"));
        sendEvent(b[5] = new TxnEventB("id5", seconds * 1000));

        listener.reset();

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 1 * 60 * 1000)); // Expire "id0" from window
        assertFalse(listener.isInvoked());

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 2 * 60 * 1000)); // Expire "id1" from window
        assertReceivedEvent(a[1], null);

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 3 * 60 * 1000));
        assertReceivedEvent(null, b[2]);

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 4 * 60 * 1000));
        assertFalse(listener.isInvoked());

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 5 * 60 * 1000));
        assertFalse(listener.isInvoked());

        sendEvent(new CurrentTimeEvent(TIME_WINDOW_SIZE_MSEC + 6 * 60 * 1000));
        assertReceivedTwoEvents(a[5], b[5]);
    }

    private void assertReceivedEvent(TxnEventA expectedA, TxnEventB expectedB) {
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(1, listener.getLastOldData().length);
        EventBean combinedEvent = listener.getLastOldData()[0];
        compare(combinedEvent, expectedA, expectedB);
        listener.reset();
    }

    private void assertReceivedTwoEvents(TxnEventA expectedA, TxnEventB expectedB) {
        assertEquals(1, listener.getOldDataList().size());
        assertEquals(2, listener.getLastOldData().length);

        // The order is not guaranteed
        if (listener.getLastOldData()[0].get("A") == expectedA) {
            compare(listener.getLastOldData()[0], expectedA, null);
            compare(listener.getLastOldData()[1], null, expectedB);
        } else {
            compare(listener.getLastOldData()[1], expectedA, null);
            compare(listener.getLastOldData()[0], null, expectedB);
        }

        listener.reset();
    }

    private static void compare(EventBean combinedEvent, TxnEventA expectedA, TxnEventB expectedB) {
        assertSame(expectedA, combinedEvent.get("A"));
        assertSame(expectedB, combinedEvent.get("B"));
        assertNull(combinedEvent.get("C"));
    }
}
