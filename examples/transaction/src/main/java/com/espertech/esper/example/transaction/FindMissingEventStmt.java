/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;

public class FindMissingEventStmt {
    public final static int TIME_WINDOW_TXNC_IN_SEC = 60 * 60;

    private EPStatement statement;

    //
    // We need to detect a transaction that did not make it through all three events.
    // In other words, a transaction with events A or B, but not C.
    // Note that, in this case, what we care about is event C.
    // The lack of events A or B could indicate a failure in the event transport and should be ignored.
    // Although the lack of an event C could also be a transport failure, it merits looking into.
    //
    public FindMissingEventStmt(EPAdministrator admin) {
        // The inner table to both A and B is C.
        //
        // The listener will consider old events generated when either A or B leave the window, with
        // a window size for A and B of 30 minutes.
        //
        // The window of C is declared large to ensure the C events don't leave the window before A and B
        // thus generating false alerts, making these obvious via timestamp. Lets keep 1 hour of data for C.
        String stmt = "select irstream * from " +
                "TxnEventA#time(30 min) A " +
                "full outer join " +
                "TxnEventC#time(1 hour) C on A.transactionId = C.transactionId " +
                "full outer join " +
                "TxnEventB#time(30 min) B on B.transactionId = C.transactionId " +
                "where C.transactionId is null";

        statement = admin.createEPL(stmt);
    }

    public void addListener(UpdateListener listener) {
        statement.addListener(listener);
    }
}
