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
import com.espertech.esper.common.internal.epl.index.base.MultiIndexEventTable;

import java.util.Iterator;

public class HistoricalIndexLookupStrategyMulti implements HistoricalIndexLookupStrategy {
    private int indexUsed;
    private HistoricalIndexLookupStrategy innerLookupStrategy;

    public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] index, ExprEvaluatorContext context) {
        if (index[0] instanceof MultiIndexEventTable) {
            MultiIndexEventTable multiIndex = (MultiIndexEventTable) index[0];
            EventTable indexToUse = multiIndex.getTables()[indexUsed];
            return innerLookupStrategy.lookup(lookupEvent, new EventTable[]{indexToUse}, context);
        }
        return index[0].iterator();
    }

    public void setIndexUsed(int indexUsed) {
        this.indexUsed = indexUsed;
    }

    public void setInnerLookupStrategy(HistoricalIndexLookupStrategy innerLookupStrategy) {
        this.innerLookupStrategy = innerLookupStrategy;
    }
}
