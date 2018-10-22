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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;

/**
 * Plan to perform an indexed table lookup.
 */
public class CompositeTableLookupPlanFactory extends TableLookupPlan {
    private final ExprEvaluator hashKeys;
    private final QueryGraphValueEntryRange[] rangeKeyPairs;

    public CompositeTableLookupPlanFactory(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum, ExprEvaluator hashKeys, QueryGraphValueEntryRange[] rangeKeyPairs) {
        super(lookupStream, indexedStream, indexNum);
        this.hashKeys = hashKeys;
        this.rangeKeyPairs = rangeKeyPairs;
    }

    protected JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTables, EventType[] eventTypes) {
        PropertyCompositeEventTable index = (PropertyCompositeEventTable) eventTables[0];
        return new CompositeTableLookupStrategy(eventTypes[this.getLookupStream()], this.getLookupStream(),
                hashKeys, rangeKeyPairs, index);
    }
}
