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
package com.espertech.esper.common.internal.epl.join.indexlookupplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;

/**
 * Plan for a full table scan.
 */
public class FullTableScanLookupPlanFactory extends TableLookupPlan {
    public FullTableScanLookupPlanFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexes) {
        super(lookupStream, indexedStream, indexes);
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        UnindexedEventTable index = (UnindexedEventTable) eventTable[0];
        return new FullTableScanLookupStrategy(index);
    }

    public String toString() {
        return "FullTableScanLookupPlan " +
                super.toString();
    }
}
