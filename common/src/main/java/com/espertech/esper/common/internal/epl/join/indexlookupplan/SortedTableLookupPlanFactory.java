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
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.sorted.SortedTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;

/**
 * Plan to perform an indexed table lookup.
 */
public class SortedTableLookupPlanFactory extends TableLookupPlan {
    protected final QueryGraphValueEntryRange rangeKeyPair;

    public SortedTableLookupPlanFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum, QueryGraphValueEntryRange rangeKeyPair) {
        super(lookupStream, indexedStream, indexNum);
        this.rangeKeyPair = rangeKeyPair;
    }

    protected JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTables, EventType[] eventTypes) {
        PropertySortedEventTable index = (PropertySortedEventTable) eventTables[0];
        return new SortedTableLookupStrategy(lookupStream, -1, rangeKeyPair, index);
    }
}
