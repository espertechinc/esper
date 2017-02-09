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

public class CombinedEventStmt {
    private EPStatement statement;

    public CombinedEventStmt(EPAdministrator admin) {
        // We need to take in events A, B and C and produce a single, combined event
        String stmt = "insert into CombinedEvent(transactionId, customerId, supplierId, latencyAC, latencyBC, latencyAB)" +
                "select C.transactionId," +
                "customerId," +
                "supplierId," +
                "C.timestamp - A.timestamp," +
                "C.timestamp - B.timestamp," +
                "B.timestamp - A.timestamp " +
                "from TxnEventA#time(30 min) A," +
                "TxnEventB#time(30 min) B," +
                "TxnEventC#time(30 min) C " +
                "where A.transactionId = B.transactionId and B.transactionId = C.transactionId";

        statement = admin.createEPL(stmt);
    }

    public void addListener(UpdateListener listener) {
        statement.addListener(listener);
    }
}
