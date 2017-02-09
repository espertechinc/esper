package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;

public class TestCombinedEventStmt extends TestStmtBase {
    private SupportUpdateListener listener;

    public void setUp() {
        super.setUp();

        listener = new SupportUpdateListener();
        CombinedEventStmt stmt = new CombinedEventStmt(epService.getEPAdministrator());
        stmt.addListener(listener);
    }

    public void testFlow() {
        TxnEventA a = new TxnEventA("id1", 1, "c1");
        TxnEventB b = new TxnEventB("id1", 2);
        TxnEventC c = new TxnEventC("id1", 3, "s1");

        // send 3 events with C last
        sendEvent(a);
        sendEvent(b);
        assertFalse(listener.isInvoked());
        sendEvent(c);
        assertCombinedEvent(a, b, c);

        // send events not matching id
        a = new TxnEventA("id4", 4, "c2");
        b = new TxnEventB("id2", 5);
        c = new TxnEventC("id3", 6, "s2");
        sendEvent(a);
        sendEvent(b);
        assertFalse(listener.isInvoked());
        sendEvent(c);

        // send events with B last
        a = new TxnEventA("id3", 7, "c2");
        b = new TxnEventB("id3", 8);
        sendEvent(a);
        assertFalse(listener.isInvoked());
        sendEvent(b);
        assertCombinedEvent(a, b, c);

        // send events with A last
        a = new TxnEventA("id6", 9, "c2");
        b = new TxnEventB("id6", 10);
        c = new TxnEventC("id6", 11, "s2");
        sendEvent(b);
        sendEvent(c);
        assertFalse(listener.isInvoked());
        sendEvent(a);
        assertCombinedEvent(a, b, c);
    }

    private void assertCombinedEvent(TxnEventA expectedA, TxnEventB expectedB, TxnEventC expectedC) {
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getLastNewData().length);
        EventBean combinedEvent = listener.getLastNewData()[0];
        assertSame(expectedC.getTransactionId(), combinedEvent.get("transactionId"));
        assertSame(expectedB.getTransactionId(), combinedEvent.get("transactionId"));
        assertSame(expectedA.getTransactionId(), combinedEvent.get("transactionId"));
        assertSame(expectedA.getCustomerId(), combinedEvent.get("customerId"));
        assertSame(expectedC.getSupplierId(), combinedEvent.get("supplierId"));
        assertSame(expectedC.getTimestamp() - expectedA.getTimestamp(), combinedEvent.get("latencyAC"));
        assertSame(expectedB.getTimestamp() - expectedA.getTimestamp(), combinedEvent.get("latencyAB"));
        assertSame(expectedC.getTimestamp() - expectedB.getTimestamp(), combinedEvent.get("latencyBC"));
        listener.reset();
    }
}
