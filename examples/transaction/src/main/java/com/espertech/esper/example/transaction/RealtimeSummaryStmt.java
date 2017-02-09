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

public class RealtimeSummaryStmt {
    private EPStatement totalsStatement;
    private EPStatement byCustomerStatement;
    private EPStatement bySupplierStatement;

    public RealtimeSummaryStmt(EPAdministrator admin) {
        //
        // Min,Max,Average total latency from the events (difference in time between A and C) over the past 30 minutes.
        // Min,Max,Average latency between events A/B (time stamp of B minus A) and B/C (time stamp of C minus B).
        //
        String stmtTotal = "select min(latencyAC) as minLatencyAC, " +
                "max(latencyAC) as maxLatencyAC, " +
                "avg(latencyAC) as avgLatencyAC, " +
                "min(latencyAB) as minLatencyAB, " +
                "max(latencyAB) as maxLatencyAB, " +
                "avg(latencyAB) as avgLatencyAB, " +
                "min(latencyBC) as minLatencyBC, " +
                "max(latencyBC) as maxLatencyBC, " +
                "avg(latencyBC) as avgLatencyBC " +
                "from CombinedEvent#time(30 min)";

        totalsStatement = admin.createEPL(stmtTotal);

        //
        // Min,Max,Average latency grouped by (a) customer ID and (b) supplier ID.
        // In other words, metrics on the the latency of the orders coming from each customer and going to each supplier.
        //
        String stmtCustomer = "select customerId," +
                "min(latencyAC) as minLatency," +
                "max(latencyAC) as maxLatency," +
                "avg(latencyAC) as avgLatency " +
                "from CombinedEvent#time(30 min) " +
                "group by customerId";

        byCustomerStatement = admin.createEPL(stmtCustomer);

        String stmtSupplier = "select supplierId," +
                "min(latencyAC) as minLatency," +
                "max(latencyAC) as maxLatency," +
                "avg(latencyAC) as avgLatency " +
                "from CombinedEvent#time(30 min) " +
                "group by supplierId";

        bySupplierStatement = admin.createEPL(stmtSupplier);
    }

    public void addTotalsListener(UpdateListener listener) {
        totalsStatement.addListener(listener);
    }

    public void addByCustomerListener(UpdateListener listener) {
        byCustomerStatement.addListener(listener);
    }

    public void addBySupplierListener(UpdateListener listener) {
        bySupplierStatement.addListener(listener);
    }
}
