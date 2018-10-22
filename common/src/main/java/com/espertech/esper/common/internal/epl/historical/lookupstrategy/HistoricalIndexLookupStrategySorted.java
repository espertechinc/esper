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
package com.espertech.esper.common.internal.epl.historical.lookupstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.sorted.SortedAccessStrategy;
import com.espertech.esper.common.internal.epl.join.exec.sorted.SortedAccessStrategyFactory;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;

import java.util.Iterator;
import java.util.Set;

public class HistoricalIndexLookupStrategySorted implements HistoricalIndexLookupStrategy {

    private int lookupStream;
    private QueryGraphValueEntryRange evalRange;
    private SortedAccessStrategy strategy;

    public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] index, ExprEvaluatorContext context) {
        if (index[0] instanceof PropertySortedEventTable) {
            PropertySortedEventTable idx = (PropertySortedEventTable) index[0];
            Set<EventBean> events = strategy.lookup(lookupEvent, idx, context);
            if (events != null) {
                return events.iterator();
            }
            return null;
        }
        return index[0].iterator();
    }

    public void setLookupStream(int lookupStream) {
        this.lookupStream = lookupStream;
    }

    public void setEvalRange(QueryGraphValueEntryRange evalRange) {
        this.evalRange = evalRange;
    }

    public void init() {
        strategy = SortedAccessStrategyFactory.make(false, lookupStream, -1, evalRange);
    }
}
